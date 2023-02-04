.POSIX:
.SUFFIXES:

.PHONY: default
default: web

.PHONY: web
web:
	clj -M -m snippetbox.core
