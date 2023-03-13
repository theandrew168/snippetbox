# snippetbox
Alex Edwards' snippetbox application in Clojure

## Setup
This project depends on the [Clojure programming language](https://clojure.org/).
I like to use a [POSIX-compatible Makefile](https://pubs.opengroup.org/onlinepubs/9699919799.2018edition/utilities/make.html) to facilitate the various project operations but traditional [clj commands](https://clojure.org/guides/deps_and_cli) will work just as well.

## Building
To build the application into a standalone JAR, run:
```
make
```

## Local Development
### Running
To start the web server:
```
make web
```

### Testing
Unit and integration tests can be ran after starting the aforementioned services:
```
make test
```
