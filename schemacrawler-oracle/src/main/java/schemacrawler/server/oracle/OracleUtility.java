package schemacrawler.server.oracle;

import com.google.common.io.Resources;
import schemacrawler.crawl.SchemaCrawler;
import schemacrawler.utility.SchemaCrawlerUtility;

import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;

/**
 * Created by answer on 2017/9/10.
 */
public class OracleUtility {
    public static String sql = "" +
            "  SELECT " +
            "  NULL AS ROUTINE_CATALOG," +
            "  PROCEDURES.OWNER AS ROUTINE_SCHEMA," +
            "  PROCEDURES.OBJECT_NAME AS ROUTINE_NAME," +
            "  PROCEDURES.OBJECT_NAME AS SPECIFIC_NAME," +
            "  'SQL' AS ROUTINE_BODY," +
            "  DBMS_METADATA.GET_DDL(OBJECT_TYPE, PROCEDURES.OBJECT_NAME, PROCEDURES.OWNER) " +
            "    AS ROUTINE_DEFINITION" +
            " FROM" +
            "  ALL_PROCEDURES PROCEDURES" +
            " WHERE" +
            "  PROCEDURES.OWNER NOT IN " +
            "    ('ANONYMOUS', 'APEX_PUBLIC_USER', 'APPQOSSYS', 'BI', 'CTXSYS', 'DBSNMP', 'DIP', " +
            "    'EXFSYS', 'FLOWS_30000', 'FLOWS_FILES', 'HR', 'IX', 'LBACSYS', " +
            "    'MDDATA', 'MDSYS', 'MGMT_VIEW', 'OE', 'OLAPSYS', 'ORACLE_OCM', " +
            "    'ORDPLUGINS', 'ORDSYS', 'OUTLN', 'OWBSYS', 'PM', 'SCOTT', 'SH', " +
            "    'SI_INFORMTN_SCHEMA', 'SPATIAL_CSW_ADMIN_USR', 'SPATIAL_WFS_ADMIN_USR', " +
            "    'SYS', 'SYSMAN', 'SYSTEM', 'TSMSYS', 'WKPROXY', 'WKSYS', 'WK_TEST', " +
            "    'WMSYS', 'XDB', 'XS$NULL', 'RDSADMIN')  " +
            "  AND NOT REGEXP_LIKE(PROCEDURES.OWNER, '^APEX_[0-9]{6}$')" +
            "  AND NOT REGEXP_LIKE(PROCEDURES.OWNER, '^FLOWS_[0-9]{5,6}$')" +
            "  AND REGEXP_LIKE(PROCEDURES.OWNER, '%s')" +
            "  AND OBJECT_TYPE IN ('PACKAGE', 'TYPE')" +
            " ORDER BY" +
            "  ROUTINE_SCHEMA," +
            "  ROUTINE_NAME";

    public static void getDDL(Connection connection, String schemaname) {

        try {
            String sql = String.format(OracleUtility.sql, schemaname);
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(sql);
            while(resultSet.next()){
                System.out.println(resultSet.getString("ROUTINE_DEFINITION"));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

    }
}
