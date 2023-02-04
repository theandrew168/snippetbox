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
### Services
This project depends on various services:
* [PostgreSQL](https://www.postgresql.org/) - for persistent storage

To develop locally, you'll need to run these services locally somehow or another.
I find [Docker](https://www.docker.com/) to be a nice tool for this but you can do whatever works best.

The following command starts the necessary containers:
```
docker compose up -d
```

These containers can be stopped via:
```
docker compose down
```

### Running
To start the web server:
```
make web
```

To apply any pending database migrations:
```
make migrate
```

### Testing
Unit and integration tests can be ran after starting the aforementioned services:
```
make test
```
