FROM clojure:tools-deps-alpine AS build
RUN adduser -D atemoia
USER atemoia
ADD --chown=atemoia . /srv/atemoia
WORKDIR /srv/atemoia
RUN clojure -Spath && echo "java -cp \"$(clojure -Spath)\" clojure.main --report stderr -m br.com.souenzzo.atemoia" | tee /srv/atemoia/start.sh

FROM openjdk:alpine
COPY --from=build /srv/atemoia /srv/atemoia
# COPY --from=build /root/.gitlibs /root/.gitlibs
COPY --from=build /home/atemoia/.m2 /home/atemoia/.m2
RUN adduser -D atemoia
USER atemoia
WORKDIR /srv/atemoia
CMD sh start.sh
