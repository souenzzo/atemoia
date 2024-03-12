FROM clojure:tools-deps
RUN apt-get update && apt-get install procps -y

