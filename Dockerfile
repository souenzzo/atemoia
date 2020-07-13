FROM clojure:tools-deps-alpine AS build
RUN apk add make git
RUN adduser -D atemoia
USER atemoia
ADD --chown=atemoia . /home/atemoia
RUN git clone https://git.heroku.com/stormy-oasis-99676.git
RUN cd /home/atemoia && make clean start.sh

FROM openjdk:alpine
RUN adduser -D atemoia
USER atemoia
COPY --from=build /home/atemoia /home/atemoia
WORKDIR /home/atemoia
CMD ["/home/atemoia/start.sh"]
