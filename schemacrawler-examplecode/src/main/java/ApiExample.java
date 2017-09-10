import static us.fatehi.commandlineparser.CommandLineUtility.applyApplicationLogLevel;
import static us.fatehi.commandlineparser.CommandLineUtility.logSystemProperties;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.logging.Level;

import javax.sql.DataSource;

import schemacrawler.schema.*;
import schemacrawler.schemacrawler.*;
import schemacrawler.server.oracle.OracleUtility;
import schemacrawler.utility.SchemaCrawlerUtility;

public final class ApiExample {

    public static void main(final String[] args)
            throws Exception {

        OracleUtility.getDDL(getConnection(), "HZ");

        // Turn application logging on by applying the correct log level
        applyApplicationLogLevel(Level.ALL);
        // Log system properties and classpath
        logSystemProperties();


        // Create the options
        final SchemaCrawlerOptions options = new SchemaCrawlerOptions();
        // Set what details are required in the schema - this affects the
        // time taken to crawl the schema
        options.setSchemaInfoLevel(SchemaInfoLevelBuilder.minimum());
        options.setRoutineInclusionRule(new IncludeAll());
        options
                .setSchemaInclusionRule(new RegularExpressionInclusionRule("HZ"));

        // Get the schema definition
    final Catalog catalog = SchemaCrawlerUtility.getCatalog(getConnection(),
                                                            options);

        for (final Schema schema : catalog.getSchemas()) {
            System.out.println(schema);
            for (final Table table : catalog.getTables(schema)) {
                System.out.print("o--> " + table);
                if (table instanceof View) {
                    System.out.println(" (VIEW)");
                } else {
                    System.out.println();
                }

                for (final Column column : table.getColumns()) {
                    System.out.println("     o--> " + column + " ("
                            + column.getColumnDataType() + ")");
                }
            }
        }

    }

    private static Connection getConnection()
            throws SchemaCrawlerException, SQLException {
        final DataSource dataSource = new DatabaseConnectionOptions("jdbc:oracle:thin:@192.168.99.100:49161:xe");
        return dataSource.getConnection("hz", "oracle");
    }

}
