FROM clojure:tools-deps-alpine AS build
RUN apk add make git
RUN adduser -D atemoia
USER atemoia
ADD --chown=atemoia . /home/atemoia
RUN cd /home/atemoia && ls -lah && ls -Ralh ./**  && make clean start.sh

FROM openjdk:alpine
RUN adduser -D atemoia
USER atemoia
COPY --from=build /home/atemoia /home/atemoia
WORKDIR /home/atemoia
CMD ["/home/atemoia/start.sh"]
