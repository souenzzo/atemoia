FROM docker.io/node:alpine AS node
RUN adduser -D atemoia
USER atemoia
WORKDIR /home/atemoia
COPY --chown=atemoia package.json package-lock.json ./
RUN npm run ci

FROM docker.io/clojure:temurin-26-tools-deps-alpine AS clojure
RUN adduser -D atemoia
USER atemoia
WORKDIR /home/atemoia
COPY --chown=atemoia ./deps.edn ./
RUN clojure -X:dev:deps prep && clojure -X:dev:deps prep
COPY --chown=atemoia . .
COPY --from=node --chown=atemoia /home/atemoia/node_modules node_modules
RUN clojure -M:dev -m atemoia.build

FROM docker.io/eclipse-temurin:26-jre-alpine
COPY --from=clojure /home/atemoia/target/atemoia.jar /usr/share/atemoia/atemoia.jar
RUN adduser -D atemoia
USER atemoia
WORKDIR /home/atemoia
CMD java \
  -Datemoia.server.http-port="$PORT" \
  -Datemoia.server.atm-db-url="$DATABASE_URL" \
  -jar /usr/share/atemoia/atemoia.jar
