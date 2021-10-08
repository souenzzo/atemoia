# atemoia

> A simple full-stack clojure app

Learn more about [clojure project structure](https://souenzzo.com.br/creating-a-clojure-project.html).

Checkout the [live demo](https://atemoia.herokuapp.com/).

This is a simple fullstack clojure application developed to explain how to deploy a clojure app.

# Overview

It consists in a backend in `src/atemoia/server.clj`. This backend has JSON and HTML endpoints

A frontend in `src/atemoia/client.cljs`. This is a react app that interacts with the JSON endpoints There is a build
script in `dev/atemoia/build.clj`. This is invoked via `clj -A:dev -M -m atemoia.build`

The build script compiles clojurescript to a minified bundle and compiles clojure to java classes. Then generates a JAR
file that include both java class files and javascript static assets.

There is a `Dockerfile` that uses `node:alpine` image to install `npm` dependencies, then use `clojure:alpine` image to
compile and create the `jar` file, then create an `openjdk:alpine` image with just the jar file as "final product".

Heroku operates in `container` mode, as described in `heroku.yml`. There is a github integration that triggers the
deploy

# Developing

Before start, install your npm dependencies with `npm install`

You can start your REPL using `clj -A:dev`

In the repl, `(require '(atemoia.server))` then `(in-ns 'atemoia.server)`. Call `(dev-main)` and after some seconds the
application should be available in localhost:8080.

You will need a postgresql server running

```shell
docker run --name my-postgres --env=POSTGRES_PASSWORD=postgres --rm -p 5432:5432 postgres:alpine
```

You can change `src/atemoia/server.clj` and run `(require 'atemoia.server :reload)` to see your changes.

Some changes (in the HTTP server) will need to call `(dev-main)` again

Changes in `src/atemoia/client.cljs` are hot reloaded.

You will never need to restart your REPL.

# atemoia.build

This namespace has a single `-main` function that when called, generate a `jar` file in `target/atemoia.jar`.

The `jar` contains everything that is listed in `target/classes`. This directory will only exists after you call this
function.

The function will

- Delete the `target` dir
- Start `shadow-cljs` server, generate a production bundle of `atemoia.client` in `target/classes/public`, as described
  in `shadow-cljs.edn`, stop `shadow-cljs` server
- Write the `pom` file and others `jar` metadata files in `target/classes/META-INF`
- Compile every clojure namespaces found in `src` and every required dependency into `target/classes`
- Create a uberjar file that has `atemoia.server` and entrypoint.

Create a uberjar is not a complex thing. It's simply `zip` the `target/classes` folder into a `.jar`, keeping it tree structure.

You can open the jar and compare with `target/classes` to have a better understanding of this process (they should be equal)


# FAQ

- How long will my bullet point remain here?

Up to 10 elements. This [code](https://github.com/souenzzo/atemoia/blob/master/src/atemoia/server.clj#L53) do that.

- Why pedestal?

Is the one that I like to use. The main feature that I like that no other http server does is the `io.pedestal.test/response-for` helper.

- Why not datomic?

The idea is be easy to deploy on heroku. Heroku gives me Postgres, so I'm ok to use it.


---      

In case of doubts or errors, report an issue

If you prefer talk [directly with me](https://t.me/souenzzo)
