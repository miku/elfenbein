#!/usr/bin/env bash

type solr >/dev/null 2>&1 || {
    echo >&2 "solr script required. see README.md"; exit 1;
}

solr `pwd`
