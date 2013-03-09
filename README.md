Elfenbein
=========

Solr + Triples + Dropdowns + Forms


Getting started
---------------

You'll need a current clone of Apache's lucene-solr project to provide
the necessary libraries (which aren't included here - yet).

Place (or symlink) all libraries listed in `spo/lib/CHECKLIST` into `spo/lib`.
From there, `solrconfig.xml` will pick it up by default.

On OS X (homebrew), there is small convenience script to start the
server using embedded Jetty:

    #!/bin/sh
    if [ -z "$1" ]; then
      echo "Usage: $ solr path/to/config/dir"
    else
      cd /usr/local/Cellar/solr/4.1.0/libexec/example && \
          java -server $JAVA_OPTS -Dsolr.solr.home=$1 -jar start.jar
    fi

We make use of this script in our own startup script, so make sure you
have something named `solr` in your path that does what the above snippet
does (a better way for Linux coming soon).

To start elfenbein (spo solr),

    $ bin/elfenbein

To get your turtle file (.ttl) into the index, consult the `bin/trainman`.

    $ bin/trainman --help
    usage: trainman [-b <N>] [--destroy-index] [-h] [-i <FILE>] [--ping] [-s
           <URL>]
     -b,--batch-size <N>   batch size for commits (10000)
        --destroy-index    destroy the whole index
     -h,--help             show help
     -i,--infile <FILE>    read and index N-triples file into Solr
        --ping             ping solr
     -s,--solr <URL>       solr server URL (http://localhost:8983/solr)


Performance evaluation
----------------------

    Server: Jetty
    Batchsize: 10000
    Documents: 476978
    Storage: SSD
    Time: 828 s
    Mean Rate: 575.59 triples/s
    Details: exports/measurements/triples_2013_03_08_11_26_13.csv

----

    Server: Jetty
    Batchsize: 10000
    Documents: 476978
    Storage: HDD
    Time: 845 s
    Mean Rate: 564.14 triples/s
    Details: exports/measurements/triples_2013_03_08_12_04_26.csv

----

Using a vanilla MySQL DB and a
SOLR [DataImportHandler](http://wiki.apache.org/solr/DataImportHandler):

    Documents: 476978
    Storage: SSD
    Time: 220 s
    Mean Rate: 2164.42 triples/s
    Details: N/A

Plus and additional

    Time: 66.742 s

for the import, which is started via:

    $ curl -XGET -v http://localhost:8983/solr/dataimport?command=full-import
    <?xml version="1.0" encoding="UTF-8"?>
    <response>
      <lst name="responseHeader">
        <int name="status">0</int>
        <int name="QTime">27</int>
      </lst>
      <lst name="initArgs">
        <lst name="defaults">
          <str name="config">db-data-config.xml</str>
          <str name="update.chain">dedupe</str>
        </lst>
      </lst>
      <str name="command">full-import</str>
      <str name="status">idle</str>
      <str name="importResponse"/>
      <lst name="statusMessages"/>
      <str name="WARNING">This response format is experimental.  It is likely to change in the future.</str>
    </response>


Development notes
-----------------

In trainman's pom.xml we build something like a fat-script, which is a
shell script that contains the project and all dependencies.

    $ head -2 projects/trainman/target/trainman
    #!/bin/sh
    exec java -server -Xmx1024m -Xms1024m -jar $0 "$@"
    < --- JAR CONTENT HERE --- >

If the `pox.xml` is autoformatted, the bash header gets scrambled. Just note.
