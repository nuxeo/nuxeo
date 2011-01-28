/*
 * (C) Copyright 2008-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Florent Guillaume
 */

package org.nuxeo.ecm.core.storage.sql.db.dialect;

import java.sql.DatabaseMetaData;
import java.sql.Types;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.hibernate.dialect.SQLServerDialect;
import org.nuxeo.ecm.core.storage.StorageException;
import org.nuxeo.ecm.core.storage.sql.Model;
import org.nuxeo.ecm.core.storage.sql.RepositoryDescriptor;
import org.nuxeo.ecm.core.storage.sql.db.Column;
import org.nuxeo.ecm.core.storage.sql.db.Database;

/**
 * Microsoft SQL Server-specific dialect.
 *
 * @author Florent Guillaume
 */
public class DialectSQLServer extends Dialect {

    private static final String DEFAULT_FULLTEXT_ANALYZER = "english";

    private static final String DEFAULT_FULLTEXT_CATALOG = "nuxeo";

    protected final String fulltextAnalyzer;

    protected final String fulltextCatalog;

    public DialectSQLServer(DatabaseMetaData metadata,
            RepositoryDescriptor repositoryDescriptor) throws StorageException {
        super(new SQLServerDialect(), metadata);
        fulltextAnalyzer = repositoryDescriptor.fulltextAnalyzer == null ? DEFAULT_FULLTEXT_ANALYZER
                : repositoryDescriptor.fulltextAnalyzer;
        fulltextCatalog = repositoryDescriptor.fulltextCatalog == null ? DEFAULT_FULLTEXT_CATALOG
                : repositoryDescriptor.fulltextCatalog;

    }

    @Override
    public String getTypeName(int sqlType, int length, int precision, int scale) {
        if (sqlType == Column.ExtendedTypes.FULLTEXT) {
            sqlType = Types.CLOB;
        }
        if (sqlType == Types.VARCHAR) {
            return "NVARCHAR(" + length + ')';
        } else if (sqlType == Types.CLOB) {
            return "NVARCHAR(MAX)";
        }
        return super.getTypeName(sqlType, length, precision, scale);
    }

    @Override
    public String getCreateFulltextIndexSql(String indexName, String tableName,
            List<String> columnNames) {
        StringBuilder buf = new StringBuilder();
        buf.append(String.format("CREATE FULLTEXT INDEX ON %s (", tableName));
        Iterator<String> it = columnNames.iterator();
        while (it.hasNext()) {
            buf.append(String.format("%s LANGUAGE %s", it.next(),
                    getQuotedFulltextAnalyzer()));
            if (it.hasNext()) {
                buf.append(", ");
            }
        }
        String fulltextUniqueIndex = "[fulltext_pk]";
        buf.append(String.format(") KEY INDEX %s ON [%s]", fulltextUniqueIndex,
                fulltextCatalog));
        return buf.toString();
    }

    @Override
    public String[] getFulltextMatch(Column ftColumn, Column mainColumn,
            String fulltextQuery) {
        String whereExpr = String.format(
                "FREETEXT([fulltext].*, ?, LANGUAGE %s)",
                getQuotedFulltextAnalyzer());
        return new String[] { null, null, whereExpr, fulltextQuery };
    }

    protected String getQuotedFulltextAnalyzer() {
        if (!Character.isDigit(fulltextAnalyzer.charAt(0))) {
            return String.format("'%s'", fulltextAnalyzer);
        }
        return fulltextAnalyzer;
    }

    @Override
    public int getFulltextIndexedColumns() {
        return 2;
    }

    @Override
    public boolean supportsCircularCascadeDeleteConstraints() {
        return false;
    }

    @Override
    public boolean supportsUpdateFrom() {
        return true;
    }

    @Override
    public boolean doesUpdateFromRepeatSelf() {
        return true;
    }

    @Override
    public boolean needsAliasForDerivedTable() {
        return true;
    }

    @Override
    public String getSecurityCheckSql(String idColumnName) {
        return String.format("dbo.NX_ACCESS_ALLOWED(%s, ?, ?) = 1",
                idColumnName);
    }

    @Override
    public String getInTreeSql(String idColumnName) {
        return String.format("dbo.NX_IN_TREE(%s, ?) = 1", idColumnName);
    }

    @Override
    public Collection<ConditionalStatement> getConditionalStatements(
            Model model, Database database) {
        String idType;
        switch (model.idGenPolicy) {
        case APP_UUID:
            idType = "NVARCHAR(36)";
            break;
        case DB_IDENTITY:
            idType = "INTEGER";
            break;
        default:
            throw new AssertionError(model.idGenPolicy);
        }

        List<ConditionalStatement> statements = new LinkedList<ConditionalStatement>();

        statements.add(new ConditionalStatement( //
                true, // early
                null, // do a check
                // strange inverted condition because this is designed to
                // test drops
                String.format(
                        "IF EXISTS(SELECT name FROM sys.fulltext_catalogs WHERE name = '%s') "
                                + "SELECT * FROM sys.tables WHERE 1 = 0 "
                                + "ELSE SELECT 1", //
                        fulltextCatalog), //
                String.format("CREATE FULLTEXT CATALOG [%s]", fulltextCatalog), //
                "SELECT 1"));

        statements.add(new ConditionalStatement(
                false, // late
                Boolean.TRUE, // always drop
                null, //
                "IF OBJECT_ID('dbo.nxTrigCascadeDelete', 'TR') IS NOT NULL DROP TRIGGER dbo.nxTrigCascadeDelete", //
                "CREATE TRIGGER nxTrigCascadeDelete ON [hierarchy] " //
                        + "INSTEAD OF DELETE AS " //
                        + "BEGIN" //
                        + "  SET NOCOUNT ON;" //
                        + "  WITH subtree(id, parentid) AS (" //
                        + "    SELECT id, parentid" //
                        + "    FROM deleted" //
                        + "  UNION ALL" //
                        + "    SELECT h.id, h.parentid" //
                        + "    FROM [hierarchy] h" //
                        + "    JOIN subtree ON subtree.id = h.parentid" //
                        + "  )" //
                        + "  DELETE FROM [hierarchy]" //
                        + "    FROM [hierarchy] h" //
                        + "    JOIN subtree" //
                        + "    ON subtree.id = h.id; " //
                        + "END" //
        ));

        statements.add(new ConditionalStatement(
                false, // late
                Boolean.TRUE, // always drop
                null, //
                "IF OBJECT_ID('dbo.NX_ACCESS_ALLOWED', 'FN') IS NOT NULL DROP FUNCTION dbo.NX_ACCESS_ALLOWED", //
                String.format(
                        "CREATE FUNCTION NX_ACCESS_ALLOWED" //
                                + "(@id %s, @users NVARCHAR(4000), @perms NVARCHAR(4000)) " //
                                + "RETURNS TINYINT AS " //
                                + "BEGIN" //
                                + "  DECLARE @allusers NVARCHAR(4000);" //
                                + "  DECLARE @allperms NVARCHAR(4000);" //
                                + "  DECLARE @first TINYINT;" //
                                + "  DECLARE @curid %<s;" //
                                + "  DECLARE @newid %<s;" //
                                + "  DECLARE @gr TINYINT;" //
                                + "  DECLARE @pe VARCHAR(1000);" //
                                + "  DECLARE @us VARCHAR(1000);" //
                                + "  SET @allusers = N'|' + @users + N'|';" //
                                + "  SET @allperms = N'|' + @perms + N'|';" //
                                + "  SET @first = 1;" //
                                + "  SET @curid = @id;" //
                                + "  WHILE @curid IS NOT NULL BEGIN" //
                                + "    DECLARE @cur CURSOR;" //
                                + "    SET @cur = CURSOR FAST_FORWARD FOR" //
                                + "      SELECT [grant], [permission], [user] FROM [acls]" //
                                + "      WHERE [id] = @curid ORDER BY [pos];" //
                                + "    OPEN @cur;" //
                                + "    FETCH FROM @cur INTO @gr, @pe, @us;" //
                                + "    WHILE @@FETCH_STATUS = 0 BEGIN" //
                                + "      IF @allusers LIKE (N'%%|' + @us + N'|%%')" //
                                + "        AND @allperms LIKE (N'%%|' + @pe + N'|%%')" //
                                + "      BEGIN" //
                                + "        CLOSE @cur;" //
                                + "        RETURN @gr;" //
                                + "      END;" //
                                + "      FETCH FROM @cur INTO @gr, @pe, @us;" //
                                + "    END;" //
                                + "    CLOSE @cur;" //
                                + "    SET @newid = (SELECT [parentid] FROM [hierarchy] WHERE [id] = @curid);" //
                                + "    IF @first = 1 AND @newid IS NULL BEGIN" //
                                + "      SET @newid = (SELECT [versionableid] FROM [versions] WHERE [id] = @curid);" //
                                + "    END;" //
                                + "    SET @first = 0;" //
                                + "    SET @curid = @newid;" //
                                + "  END;" //
                                + "  RETURN 0; " //
                                + "END" //
                        , idType)));

        statements.add(new ConditionalStatement(
                false, // late
                Boolean.TRUE, // always drop
                null, //
                "IF OBJECT_ID('dbo.NX_IN_TREE', 'FN') IS NOT NULL DROP FUNCTION dbo.NX_IN_TREE", //
                String.format(
                        "CREATE FUNCTION NX_IN_TREE(@id %s, @baseid %<s) " //
                                + "RETURNS TINYINT AS " //
                                + "BEGIN" //
                                + "  DECLARE @curid %<s;" //
                                + "  IF @baseid IS NULL OR @id IS NULL OR @baseid = @id RETURN 0;" //
                                + "  SET @curid = @id;" //
                                + "  WHILE @curid IS NOT NULL BEGIN" //
                                + "    SET @curid = (SELECT [parentid] FROM [hierarchy] WHERE [id] = @curid);" //
                                + "    IF @curid = @baseid RETURN 1;" //
                                + "  END;" //
                                + "  RETURN 0;" //
                                + "END" //
                        , idType)));

        return statements;
    }

}
