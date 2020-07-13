ARG PROJECT=atemoia
FROM clojure:tools-deps-alpine AS build
RUN apk add make
RUN adduser -D $PROJECT
USER $PROJECT
ADD --chown=$PROJECT . /home/$PROJECT
RUN cd /home/atemoia && make clean start.sh

FROM openjdk:alpine
RUN adduser -D $PROJECT
USER $PROJECT
COPY --from=build /home/$PROJECT /home/$PROJECT
WORKDIR /home/$PROJECT
CMD ["/home/atemoia/start.sh"]
