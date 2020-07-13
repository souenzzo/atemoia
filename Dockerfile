FROM clojure:openjdk-15-alpine AS build
RUN apk add make git tree
RUN adduser -D atemoia
USER atemoia
WORKDIR /home/atemoia
COPY --chown=atemoia Makefile .
RUN make start.sh && rm -rf Makefile .gitlibs/_repos

FROM openjdk:15-alpine
RUN adduser -D atemoia
USER atemoia
COPY --from=build /home/atemoia /home/atemoia
WORKDIR /home/atemoia
CMD ["/home/atemoia/start.sh"]
