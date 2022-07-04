FROM node:alpine AS node
RUN adduser -D atemoia
USER atemoia
WORKDIR /home/atemoia
COPY --chown=atemoia package.json package-lock.json ./
RUN npm --audit=false --ignore-scripts=true --update-notifier=false --fund=false ci

FROM clojure:openjdk-18-tools-deps-alpine AS clojure
RUN apk add git nodejs
RUN adduser -D atemoia
USER atemoia
WORKDIR /home/atemoia
COPY --chown=atemoia ./deps.edn ./
RUN clojure -A:dev -P && clojure -P --report stderr
COPY --chown=atemoia . .
COPY --from=node --chown=atemoia /home/atemoia/node_modules node_modules
RUN clojure -A:dev -M -m atemoia.build

FROM openjdk:18-jdk-alpine
RUN adduser -D atemoia
USER atemoia
WORKDIR /home/atemoia
COPY --from=clojure --chown=atemoia /home/atemoia/target/atemoia.jar ./
CMD java \
  -Datemoia.server.http-port="$PORT" \
  -Datemoia.server.atm-db-url="$DATABASE_URL" \
  -jar atemoia.jar
