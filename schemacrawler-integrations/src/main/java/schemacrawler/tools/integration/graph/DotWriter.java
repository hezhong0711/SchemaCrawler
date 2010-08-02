/* 
 *
 * SchemaCrawler
 * http://sourceforge.net/projects/schemacrawler
 * Copyright (c) 2000-2010, Sualeh Fatehi.
 *
 * This library is free software; you can redistribute it and/or modify it under the terms
 * of the GNU Lesser General Public License as published by the Free Software Foundation;
 * either version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with this
 * library; if not, write to the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, MA 02111-1307, USA.
 *
 */

package schemacrawler.tools.integration.graph;


import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import schemacrawler.schema.Column;
import schemacrawler.schema.ColumnMap;
import schemacrawler.schema.DatabaseInfo;
import schemacrawler.schema.ForeignKey;
import schemacrawler.schema.JdbcDriverInfo;
import schemacrawler.schema.NamedObject;
import schemacrawler.schema.Schema;
import schemacrawler.schema.SchemaCrawlerInfo;
import schemacrawler.schema.Table;
import schemacrawler.schema.View;
import schemacrawler.schemacrawler.SchemaCrawlerException;
import schemacrawler.utility.MetaDataUtility;
import schemacrawler.utility.MetaDataUtility.Connectivity;
import sf.util.Utility;

final class DotWriter
{

  private static final Random random = new Random(DotWriter.class.getName()
    .hashCode());

  private static String nextRandomHTMLPastelColorValue()
  {
    for (int i = 0; i < random.nextInt(2000); i++)
    {
      random.nextFloat();
    }

    final float hue = random.nextFloat();
    // Saturation between 0.1 and 0.3
    final float saturation = (random.nextInt(2000) + 1000) / 10000f;
    final float luminance = 0.9f;
    final Color color = Color.getHSBColor(hue, saturation, luminance);

    return "#" + Integer.toHexString(color.getRGB()).substring(2).toUpperCase();
  }

  private final PrintWriter out;
  private final Map<Schema, String> colorMap;

  DotWriter(final File dotFile)
    throws SchemaCrawlerException
  {
    if (dotFile == null)
    {
      throw new SchemaCrawlerException("No dot file provided");
    }
    try
    {
      out = new PrintWriter(dotFile);
    }
    catch (final IOException e)
    {
      throw new SchemaCrawlerException("Cannot open dot file for output", e);
    }

    colorMap = new HashMap<Schema, String>();
  }

  public void close()
  {
    out.println("}");
    out.flush();
    //
    out.close();
  }

  public void open()
  {
    final String text = Utility.readResourceFully("/dot.header.txt");
    out.println(text);
  }

  public void print(final SchemaCrawlerInfo schemaCrawlerInfo,
                    final DatabaseInfo databaseInfo,
                    final JdbcDriverInfo jdbcDriverInfo)
  {
    final StringBuilder graphInfo = new StringBuilder();

    // SchemaCrawler info
    graphInfo
      .append("      <table border=\"1\" cellborder=\"0\" cellspacing=\"0\">")
      .append(Utility.NEWLINE);

    graphInfo.append("        <tr>").append(Utility.NEWLINE);
    graphInfo.append("          <td align=\"right\">Generated by:</td>")
      .append(Utility.NEWLINE);
    graphInfo.append("          <td align=\"left\">").append(schemaCrawlerInfo
      .getSchemaCrawlerProductName()).append(" ").append(schemaCrawlerInfo
      .getSchemaCrawlerVersion()).append("</td>").append(Utility.NEWLINE);
    graphInfo.append("        </tr>").append(Utility.NEWLINE);

    // Database info
    graphInfo.append("        <tr>").append(Utility.NEWLINE);
    graphInfo.append("          <td align=\"right\">Database:</td>")
      .append(Utility.NEWLINE);
    graphInfo.append("          <td align=\"left\">").append(databaseInfo
      .getProductName()).append("  ").append(databaseInfo.getProductVersion())
      .append("</td>").append(Utility.NEWLINE);
    graphInfo.append("        </tr>").append(Utility.NEWLINE);

    // JDBC driver info
    graphInfo.append("        <tr>").append(Utility.NEWLINE);
    graphInfo.append("          <td align=\"right\">JDBC Connection:</td>")
      .append(Utility.NEWLINE);
    graphInfo.append("          <td align=\"left\">").append(jdbcDriverInfo
      .getConnectionUrl()).append("</td>").append(Utility.NEWLINE);
    graphInfo.append("        </tr>").append(Utility.NEWLINE);

    graphInfo.append("        <tr>").append(Utility.NEWLINE);
    graphInfo.append("          <td align=\"right\">JDBC Driver:</td>")
      .append(Utility.NEWLINE);
    graphInfo.append("          <td align=\"left\">").append(jdbcDriverInfo
      .getDriverName()).append("  ").append(jdbcDriverInfo.getDriverVersion())
      .append("</td>").append(Utility.NEWLINE);
    graphInfo.append("        </tr>").append(Utility.NEWLINE);

    graphInfo.append("      </table>");

    final String graphLabel = String
      .format("  graph [%n    label=<%n%s    >%n    labeljust=r%n    labelloc=b%n  ];%n%n",
              graphInfo.toString());

    out.println(graphLabel);
  }

  public void print(final Collection<Table> tables,
                    final Collection<ColumnMap> weakAssociations)
  {
    if (tables != null)
    {
      for (final Table table: tables)
      {
        print(table);
        out.write(Utility.NEWLINE);
        // Print foreign keys
        for (final ForeignKey foreignKey: table.getForeignKeys())
        {
          final ColumnMap[] columnPairs = foreignKey.getColumnPairs();
          for (final ColumnMap columnMap: columnPairs)
          {
            if (table.equals(columnMap.getPrimaryKeyColumn().getParent()))
            {
              out.write(printColumnAssociation(foreignKey.getName(),
                                               columnMap,
                                               tables));
            }
          }
        }
        out.write(Utility.NEWLINE);
        out.write(Utility.NEWLINE);
      }
    }

    out.write(Utility.NEWLINE);
    for (final ColumnMap columnMap: weakAssociations)
    {
      out.write(printColumnAssociation("", columnMap, tables));
    }
    out.write(Utility.NEWLINE);
  }

  private String[] getPortIds(final Column column,
                              final Collection<Table> tables)
  {
    final String portIds[] = new String[2];
    if (tables.contains(column.getParent()))
    {
      portIds[0] = String.format("\"%s\":\"%s.start\"", nodeId(column
        .getParent()), nodeId(column));
      portIds[1] = String.format("\"%s\":\"%s.end\"",
                                 nodeId(column.getParent()),
                                 nodeId(column));
    }
    else
    {
      // Create new node
      String nodeId = print(column);
      //
      portIds[0] = nodeId;
      portIds[1] = nodeId;
    }
    return portIds;
  }

  private String nodeId(final NamedObject namedOjbect)
  {
    if (namedOjbect == null)
    {
      return "";
    }
    else
    {
      return Utility.convertForComparison(namedOjbect.getName()) + "_"
             + Integer.toHexString(namedOjbect.getFullName().hashCode());
    }
  }

  private String print(final Column column)
  {
    String nodeId = "\"" + UUID.randomUUID().toString() + "\"";
    String columnNode = String.format("  %s [label=<%s>];\n", nodeId, column
      .getFullName());

    out.write(columnNode);

    return nodeId;
  }

  private void print(final Table table)
  {
    final Schema schema = table.getSchema();

    final String tableNameBgColor;
    if (!colorMap.containsKey(schema))
    {
      tableNameBgColor = nextRandomHTMLPastelColorValue();
      colorMap.put(schema, tableNameBgColor);
    }
    else
    {
      tableNameBgColor = colorMap.get(schema);
    }

    final StringBuilder buffer = new StringBuilder();
    buffer.append("  /* ").append(table.getFullName())
      .append(" -=-=-=-=-=-=-=-=-=-=-=-=-=- */").append(Utility.NEWLINE);
    buffer.append("  \"").append(nodeId(table)).append("\" [")
      .append(Utility.NEWLINE).append("    label=<").append(Utility.NEWLINE);
    buffer
      .append("      <table border=\"1\" cellborder=\"0\" cellspacing=\"0\" bgcolor=\"white\">")
      .append(Utility.NEWLINE);
    buffer.append("        <tr>").append(Utility.NEWLINE);

    buffer
      .append("          <td colspan=\"2\" bgcolor=\"")
      .append(tableNameBgColor)
      .append("\" align=\"left\"><font face=\"Helvetica-Bold\" point-size=\"9\">")
      .append(table.getFullName()).append("</font></td>")
      .append(Utility.NEWLINE);
    buffer.append("          <td bgcolor=\"").append(tableNameBgColor)
      .append("\" align=\"right\">").append((table instanceof View? "[view]"
                                                                  : "[table]"))
      .append("</td>").append(Utility.NEWLINE);
    buffer.append("        </tr>").append(Utility.NEWLINE);
    for (final Column column: table.getColumns())
    {
      buffer.append("        <tr>").append(Utility.NEWLINE);
      buffer.append("          <td port=\"").append(nodeId(column))
        .append(".start\" align=\"left\">");
      if (column.isPartOfPrimaryKey())
      {
        buffer.append("<font face=\"Helvetica-BoldOblique\">");
      }
      buffer.append(column.getName());
      if (column.isPartOfPrimaryKey())
      {
        buffer.append("</font>");
      }
      buffer.append("</td>").append(Utility.NEWLINE);
      buffer.append("          <td> </td>").append(Utility.NEWLINE);
      buffer.append("          <td port=\"").append(nodeId(column))
        .append(".end\" align=\"right\">");
      buffer.append(column.getType().getDatabaseSpecificTypeName())
        .append(column.getWidth());
      buffer.append("</td>").append(Utility.NEWLINE);
      buffer.append("        </tr>").append(Utility.NEWLINE);
    }
    buffer.append("      </table>").append(Utility.NEWLINE);
    buffer.append("    >").append(Utility.NEWLINE).append("  ];")
      .append(Utility.NEWLINE);

    out.write(buffer.toString());
  }

  private String printColumnAssociation(final String associationName,
                                        final ColumnMap columnMap,
                                        final Collection<Table> tables)
  {
    final Column primaryKeyColumn = columnMap.getPrimaryKeyColumn();
    final Column foreignKeyColumn = columnMap.getForeignKeyColumn();

    final String[] pkPortIds = getPortIds(primaryKeyColumn, tables);
    final String[] fkPortIds = getPortIds(foreignKeyColumn, tables);

    final Connectivity connectivity = MetaDataUtility
      .getConnectivity(foreignKeyColumn);
    final String pkSymbol = "teetee";
    final String fkSymbol;
    if (connectivity != null)
    {
      switch (connectivity)
      {
        case OneToOne:
          fkSymbol = "teeodot";
          break;
        case OneToMany:
          fkSymbol = "crowodot";
          break;
        default:
          fkSymbol = "none";
          break;
      }
    }
    else
    {
      fkSymbol = "none";
    }
    final String style;
    if (Utility.isBlank(associationName))
    {
      style = "dashed";
    }
    else
    {
      style = "solid";
    }

    return String
      .format("  %s:w -> %s:e [label=<%s> style=\"%s\" arrowhead=\"%s\" arrowtail=\"%s\"];%n",
              pkPortIds[0],
              fkPortIds[1],
              associationName,
              style,
              fkSymbol,
              pkSymbol);
  }

}
