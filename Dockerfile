FROM clojure:temurin-21-tools-deps-alpine
RUN adduser atemoia
USER atemoia
WORKDIR /home/atemoia

