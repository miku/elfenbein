Elfenbein
=========

Solr + Triples + Dropdowns + Forms


Getting started
---------------

You'll need a current clone of Apache's lucene-solr project to provide
the necessary libraries (which aren't included here - yet).

    $ mkdir -p ~/src/apache && cd src/apache
    $ git clone git://git.apache.org/lucene-solr.git

If you setup the clone as above, your `solrconfig.xml` should be good to go.
Otherwise change the following elements in `spo/conf/solrconfig.xml` to reflect
your systems' setup:

    <lib dir="${user.home}/src/apache/lucene-solr/solr/contrib/extraction/lib" 
        regex=".*\.jar" />
    <lib dir="${user.home}/src/apache/lucene-solr/solr/build/contrib/solr-analysis-extras/lucene-libs" 
        regex=".*\.jar" />

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
See: `exports/measurements/triples_2013_03_08_11_26_13.csv`


Development notes
-----------------

In trainman's pom.xml we build something like a fat-script, which is a
shell script that contains the project and all dependencies.

    $ head -2 projects/trainman/target/trainman
    #!/bin/sh
    exec java -server -Xmx1024m -Xms1024m -jar $0 "$@"
    < --- JAR CONTENT HERE --- >

If the `pox.xml` is autoformatted, the bash header gets scrambled. Just note.
