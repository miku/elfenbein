Example payloads
================

    $ curl -XPOST localhost:8983/solr/update?commit=true \
        -H "Content-Type: text/xml" --data-binary @update0.xml
