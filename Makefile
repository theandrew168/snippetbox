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
	@echo 'TODO: run tests'

.PHONY: clean
clean:
	clj -T:build clean
