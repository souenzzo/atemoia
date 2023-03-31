FROM node:alpine AS node
RUN adduser -D atemoia
USER atemoia
WORKDIR /home/atemoia
COPY --chown=atemoia package.json package-lock.json ./
RUN npm run ci

FROM clojure:openjdk-19-tools-deps AS clojure
RUN adduser atemoia
USER atemoia
WORKDIR /home/atemoia
COPY --chown=atemoia ./deps.edn ./
RUN clojure -M:dev -P && clojure -P --report stderr
COPY --chown=atemoia . .
COPY --from=node --chown=atemoia /home/atemoia/node_modules node_modules
RUN clojure -M:dev -m atemoia.build

FROM openjdk:19-jdk
RUN adduser atemoia
USER atemoia
WORKDIR /home/atemoia
COPY --from=clojure --chown=atemoia /home/atemoia/target/atemoia.jar ./
CMD java \
  -Datemoia.server.http-port="$PORT" \
  -Datemoia.server.atm-db-url="$DATABASE_URL" \
  -jar atemoia.jar
