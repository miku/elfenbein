README
======

Is such a jar soup a solr best practice?

    $ mkdir -p ~/src/apache && cd ~/src/apache
    $ git clone git://git.apache.org/lucene-solr.git

    $ cd ~/github/miku/elfenbein/spo/lib
    $ export SOLR_CLONE=$HOME/src/apache/lucene-solr
    $ for fn in $(find $SOLR_CLONE/solr/contrib/extraction/lib -name "*jar"); \
        do ln -s $fn .; done
    $ for fn in $(find $SOLR_CLONE/solr/build/contrib/solr-analysis-extras/lucene-libs -name "*jar"); \
        do ln -s $fn .; done
    $ for fn in $(find $SOLR_CLONE/solr/build/contrib/solr-dataimporthandler -name "*jar"); \
        do ln -s $fn .; done
