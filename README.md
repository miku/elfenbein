Elfenbein
=========

Solr + Triples + Dropdowns + Forms


Getting started
---------------

You'll need a current clone of Apache's lucene-solr project to provide
the necessary libraries (which aren't included here).

    $ mkdir -p ~/src/apache && cd src/apache
    $ git clone git://git.apache.org/lucene-solr.git

If you setup the clone as above, your `solrconfig.xml` should be good to go.
Otherwise change the following elements in `solrconfig.xml` to reflect
your systems' setup:

    <lib dir="${user.home}/src/apache/lucene-solr/solr/contrib/extraction/lib" 
        regex=".*\.jar" />
    <lib dir="${user.home}/src/apache/lucene-solr/solr/build/contrib/solr-analysis-extras/lucene-libs" 
        regex=".*\.jar" />

On OS X, there is small SOLR script to start the server using embedded Jetty:

    $ solr `pwd`


The `solr` script is just:

    #!/bin/sh
    if [ -z "$1" ]; then
      echo "Usage: $ solr path/to/config/dir"
    else
      cd /usr/local/Cellar/solr/4.1.0/libexec/example && \
          java -server $JAVA_OPTS -Dsolr.solr.home=$1 -jar start.jar
    fi

Again, change `/usr/local/Cellar/solr/4.1.0/libexec/example` to reflect
your systems' setup.
