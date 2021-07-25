FROM node:alpine AS node
RUN adduser -D atemoia
USER atemoia
WORKDIR /home/atemoia
COPY --chown=atemoia package.json package-lock.json ./
RUN npm install

FROM clojure:openjdk-17-tools-deps-alpine AS clojure
RUN apk add git
RUN adduser -D atemoia
USER atemoia
WORKDIR /home/atemoia
COPY --chown=atemoia ./deps.edn ./
RUN clojure -A:dev -Spath && clojure -Spath
COPY --chown=atemoia . .
COPY --from=node --chown=atemoia /home/atemoia/node_modules node_modules
RUN clojure -A:dev -M -m atemoia.build

FROM openjdk:17-jdk-alpine
RUN adduser -D atemoia
USER atemoia
WORKDIR /home/atemoia
COPY --from=clojure --chown=atemoia /home/atemoia/target/standalone.jar ./
CMD ["java", "-jar", "standalone.jar"]
