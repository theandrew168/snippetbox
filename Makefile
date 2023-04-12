.POSIX:
.SUFFIXES:

.PHONY: default
default: build

.PHONY: build
build:
	clj -T:build jar

.PHONY: web
web:
	clj -M:run

.PHONY: migrate
migrate:
	@echo 'TODO: apply migrations'

.PHONY: test
test:
	clj -M:test

.PHONY: clean
clean:
	clj -T:build clean
