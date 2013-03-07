package de.ul.ub;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
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
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.SolrPingResponse;
import org.apache.solr.common.SolrInputDocument;
import org.semanticweb.yars.nx.Node;
import org.semanticweb.yars.nx.parser.NxParser;
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

	@SuppressWarnings("static-access")
	// cf. http://stackoverflow.com/a/12467149/89391
	// cf. https://issues.apache.org/jira/browse/CLI-224
	public static void main(String[] args) throws ParseException, IOException {

		// setup metrics (http://metrics.codahale.com/)
		// add a distinct name to collect all metrics
		DateFormat dateFormat = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");
		Date date = new Date();
		final Meter triples = Metrics.newMeter(App.class, "triples_" + dateFormat.format(date),
				"triples", TimeUnit.SECONDS);

		ConsoleReporter.enable(REPORT_INTERVAL, TimeUnit.SECONDS);

		// metrics will complain,
		// if the directory for its measurements does not exists ..
		File metricsDirectory = new File("work/measurements");
		if (!metricsDirectory.exists()) {
			metricsDirectory.mkdirs();
		}

		CsvReporter.enable(metricsDirectory, REPORT_INTERVAL, TimeUnit.SECONDS);

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
				.withArgName("N")
				.hasArg()
				.withDescription(
						"batch size for commits (" + DEFAULT_BATCH_SIZE + ")")
				.withLongOpt("batch-size").withType(Number.class).create("b"));

		options.addOption("h", "help", false, "show help");

		CommandLineParser parser = new PosixParser();
		CommandLine cmd = parser.parse(options, args);

		// default solr server
		String urlString = cmd.getOptionValue("s", DEFAULT_SOLR_URL);
		final SolrServer solr = new HttpSolrServer(urlString);

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

			// default batch size
			long batchSize = DEFAULT_BATCH_SIZE;
			if (cmd.hasOption("b")) {
				batchSize = (Long) cmd.getParsedOptionValue("b");
			}

			// make sure solr is there
			try {
				solr.ping();
			} catch (SolrServerException sse) {
				LOGGER.error(sse.getLocalizedMessage());
				System.exit(1);
			}

			String filename = cmd.getOptionValue("i");
			LOGGER.debug("loading " + filename);

			final FileInputStream is = new FileInputStream(filename);
			final NxParser nxp = new NxParser(is);

			Node[] nxx;
			long counter = 0;
			while (nxp.hasNext()) {
				nxx = (Node[]) nxp.next();
				SolrInputDocument document = new SolrInputDocument();
				document.addField("s", nxx[0]);
				document.addField("p", nxx[1]);
				document.addField("o", nxx[2]);
				try {
					solr.add(document);
					counter += 1;
					triples.mark();
				} catch (SolrServerException sse) {
					LOGGER.error(sse.getLocalizedMessage());
					System.exit(1);
				}
				if (counter % batchSize == 0) {
					try {
						solr.commit();
					} catch (SolrServerException sse) {
						LOGGER.error(sse.getLocalizedMessage());
						System.exit(1);
					}
				}
			}

			try {
				solr.commit();
			} catch (SolrServerException sse) {
				LOGGER.error(sse.getLocalizedMessage());
				System.exit(1);
			}
		}
	}
}
