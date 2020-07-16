FROM node:alpine AS node
COPY package.json .
COPY package-lock.json .
RUN npm install

FROM clojure:openjdk-15-tools-deps-alpine
RUN adduser -D atemoia
USER atemoia
WORKDIR /home/atemoia
COPY --chown=atemoia . .
COPY --from=node --chown=atemoia node_modules node_modules
RUN clojure -A:cljsbuild && mkdir classes && clojure -e "(compile 'br.com.souenzzo.atemoia)"
CMD ["clojure", "-A:app"]
