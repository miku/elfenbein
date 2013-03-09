package de.ul.ub;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.SolrPingResponse;
import org.apache.solr.common.SolrInputDocument;
import org.semanticweb.yars.nx.Node;
import org.semanticweb.yars.nx.parser.NxParser;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.Meter;
import com.yammer.metrics.reporting.ConsoleReporter;
import com.yammer.metrics.reporting.CsvReporter;

public class App {

    private static Logger LOGGER = LoggerFactory.getLogger(App.class);
    private static long DEFAULT_BATCH_SIZE = 10000;
    private static String DEFAULT_SOLR_URL = "http://localhost:8983/solr";
    private static int REPORT_INTERVAL = 10;
    private static String DEFAULT_JDBC_URL = "jdbc:mysql://localhost:3306/elfenbein";
    private static String DEFAULT_DB_USERNAME = "elfenbein";
    private static String DEFAULT_DB_PASSWORD = "elfenbein";

    @SuppressWarnings("static-access")
    // cf. http://stackoverflow.com/a/12467149/89391
    // cf. https://issues.apache.org/jira/browse/CLI-224
    public static void main(String[] args) throws ParseException, IOException {

        // setup options
        Options options = new Options();

        options.addOption(OptionBuilder.withLongOpt("destroy-index")
                .withDescription("destroy the whole index").create());

        options.addOption(OptionBuilder.withLongOpt("ping")
                .withDescription("ping solr").create());

        options.addOption(OptionBuilder.withArgName("FILE")
                .withLongOpt("infile")
                .withDescription("read and index N-triples file into Solr")
                .hasArg().create("i"));

        options.addOption(OptionBuilder.withArgName("URL").withLongOpt("solr")
                .withDescription("solr server URL (" + DEFAULT_SOLR_URL + ")")
                .hasArg().create("s"));

        options.addOption(OptionBuilder
                .withArgName("NAME")
                .withLongOpt("metrics-filename")
                .withDescription(
                        "filename for benchmarks (triples_yyyy_MM_dd_HH_mm_ss.csv)")
                .hasArg().create("m"));

        options.addOption(OptionBuilder
                .withArgName("N")
                .hasArg()
                .withDescription(
                        "batch size for commits (" + DEFAULT_BATCH_SIZE + ")")
                .withLongOpt("batch-size").withType(Number.class).create("b"));

        // database options
        options.addOption(OptionBuilder.withArgName("URL").hasArg()
                .withDescription("JDBC URL (" + DEFAULT_JDBC_URL + ")")
                .withLongOpt("jdbc").create("j"));

        options.addOption(OptionBuilder.withArgName("USERNAME").hasArg()
                .withDescription("db username (" + DEFAULT_DB_USERNAME + ")")
                .withLongOpt("username").create("u"));

        options.addOption(OptionBuilder.withArgName("PASSWORD").hasArg()
                .withDescription("db password (" + DEFAULT_DB_PASSWORD + ")")
                .withLongOpt("password").create("p"));

        options.addOption("h", "help", false, "show help");

        CommandLineParser parser = new PosixParser();
        CommandLine cmd = parser.parse(options, args);

        // setup metrics (http://metrics.codahale.com/)
        // add a distinct name to collect all metrics
        final DateFormat dateFormat = new SimpleDateFormat(
                "yyyy_MM_dd_HH_mm_ss");
        final Date date = new Date();
        String metricsFilename = "triples_" + dateFormat.format(date);
        if (cmd.hasOption("metrics-filename")) {
            metricsFilename = cmd.getOptionValue("metrics-filename");
        }
        final Meter triples = Metrics.newMeter(App.class, metricsFilename,
                "triples", TimeUnit.SECONDS);

        ConsoleReporter.enable(REPORT_INTERVAL, TimeUnit.SECONDS);

        // metrics will complain,
        // if the directory for its measurements does not exists ..
        File metricsDirectory = new File("work/measurements");
        if (!metricsDirectory.exists()) {
            metricsDirectory.mkdirs();
        }

        CsvReporter.enable(metricsDirectory, REPORT_INTERVAL, TimeUnit.SECONDS);

        // prepare solr server connection
        String urlString = cmd.getOptionValue("s", DEFAULT_SOLR_URL);
        final SolrServer solr = new HttpSolrServer(urlString);

        // setup DB connection
        String jdbcUrl = cmd.getOptionValue("jdbc", DEFAULT_JDBC_URL);
        String username = cmd.getOptionValue("username", DEFAULT_DB_USERNAME);
        String password = cmd.getOptionValue("jdbc", DEFAULT_DB_PASSWORD);

        BasicDataSource ds = new BasicDataSource();
        ds.setUsername(username);
        ds.setPassword(password);
        ds.setDriverClassName("com.mysql.jdbc.Driver");
        ds.setUrl(jdbcUrl);

        if (cmd.hasOption('h')) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("trainman", options, true);
            System.exit(0);
        }

        if (cmd.hasOption("ping")) {
            try {
                SolrPingResponse pong = solr.ping();
                System.out.println(pong.getElapsedTime() + "ms");
            } catch (SolrServerException sse) {
                LOGGER.error(sse.getLocalizedMessage());
                System.exit(1);
            }
        }

        if (cmd.hasOption("destroy-index")) {
            try {
                solr.deleteByQuery("*:*");
                solr.commit(true, true);
                System.exit(0);
            } catch (SolrServerException sse) {
                LOGGER.error(sse.getLocalizedMessage());
                System.exit(1);
            }
        }

        if (cmd.hasOption("i")) {

            final DBI dbi = new DBI(ds);
            final Handle h = dbi.open();

            String filename = cmd.getOptionValue("i");
            LOGGER.debug("loading " + filename);

            final BufferedReader in = new BufferedReader(new InputStreamReader(
                    new FileInputStream(filename), "UTF8"));
            final NxParser nxp = new NxParser(in);

            Node[] nxx;
            while (nxp.hasNext()) {
                nxx = (Node[]) nxp.next();
                h.execute("insert into triples (s, p, o) values (?, ?, ?) on duplicate key " +
                        "update s = VALUES(s), p = VALUES (p), o = VALUES(o), last_modified = NOW()",
                        nxx[0].toString(),
                        nxx[1].toString(),
                        nxx[2].toString());
                triples.mark();
            }
            h.close();
        }
    }
}
