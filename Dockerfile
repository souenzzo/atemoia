FROM clojure:openjdk-15-tools-deps-alpine
RUN adduser -D atemoia
USER atemoia
WORKDIR /home/atemoia
ADD --chown=atemoia . /home/atemoia
RUN clojure -Spath
CMD clojure --report stderr -m br.com.souenzzo.atemoia

