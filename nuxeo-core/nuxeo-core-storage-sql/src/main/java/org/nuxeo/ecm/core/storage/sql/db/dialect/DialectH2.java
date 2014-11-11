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
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.hibernate.dialect.H2Dialect;
import org.nuxeo.ecm.core.storage.StorageException;
import org.nuxeo.ecm.core.storage.sql.Model;
import org.nuxeo.ecm.core.storage.sql.RepositoryDescriptor;
import org.nuxeo.ecm.core.storage.sql.db.Column;
import org.nuxeo.ecm.core.storage.sql.db.Database;
import org.nuxeo.ecm.core.storage.sql.db.Table;

/**
 * H2-specific dialect.
 *
 * @author Florent Guillaume
 */
public class DialectH2 extends Dialect {

    private static final String DEFAULT_FULLTEXT_ANALYZER = "org.apache.lucene.analysis.standard.StandardAnalyzer";

    private final String fulltextAnalyzer;

    public DialectH2(DatabaseMetaData metadata,
            RepositoryDescriptor repositoryDescriptor) throws StorageException {
        super(new H2Dialect(), metadata);
        fulltextAnalyzer = repositoryDescriptor.fulltextAnalyzer == null ? DEFAULT_FULLTEXT_ANALYZER
                : repositoryDescriptor.fulltextAnalyzer;
    }

    @Override
    public String getCreateFulltextIndexSql(String indexName, String tableName,
            List<String> columnNames) {
        return null; // no SQL index for H2
    }

    @Override
    public String[] getFulltextMatch(Column ftColumn, Column mainColumn,
            String fulltextQuery) {
        String queryTable = String.format(
                "NXFT_SEARCH('%s', '%s', ?) %%s ON %s = %%<s.KEY", "PUBLIC",
                ftColumn.getTable().getName(), mainColumn.getFullQuotedName());
        return new String[] { queryTable, fulltextQuery, null, null };
    }

    @Override
    public int getFulltextIndexedColumns() {
        return 0;
    }

    @Override
    public boolean supportsUpdateFrom() {
        return false; // check this, unused
    }

    @Override
    public boolean doesUpdateFromRepeatSelf() {
        return true;
    }

    @Override
    public String getClobCast(boolean inOrderBy) {
        if (!inOrderBy) {
            return "CAST(%s AS VARCHAR)";
        }
        return null;
    }

    @Override
    public String getSecurityCheckSql(String idColumnName) {
        return String.format("NX_ACCESS_ALLOWED(%s, ?, ?)", idColumnName);
    }

    @Override
    public String getInTreeSql(String idColumnName) {
        return String.format("NX_IN_TREE(%s, ?)", idColumnName);
    }

    @Override
    public boolean isFulltextTableNeeded() {
        return false;
    }

    @Override
    public boolean supportsArrays() {
        return false;
    }

    private static final String h2Functions = "org.nuxeo.ecm.core.storage.sql.db.H2Functions";

    private static final String h2Fulltext = "org.nuxeo.ecm.core.storage.sql.db.H2Fulltext";

    @Override
    public Collection<ConditionalStatement> getConditionalStatements(
            Model model, Database database) {
        String methodSuffix;
        switch (model.idGenPolicy) {
        case APP_UUID:
            methodSuffix = "String";
            break;
        case DB_IDENTITY:
            methodSuffix = "Long";
            break;
        default:
            throw new AssertionError(model.idGenPolicy);
        }
        Table ft = database.getTable(model.FULLTEXT_TABLE_NAME);
        Column ftst = ft.getColumn(model.FULLTEXT_SIMPLETEXT_KEY);
        Column ftbt = ft.getColumn(model.FULLTEXT_BINARYTEXT_KEY);

        List<ConditionalStatement> statements = new LinkedList<ConditionalStatement>();

        statements.add(makeFunction("NX_IN_TREE", //
                "isInTree" + methodSuffix));

        statements.add(makeFunction("NX_ACCESS_ALLOWED", //
                "isAccessAllowed" + methodSuffix));

        statements.add(new ConditionalStatement( //
                false, // late
                Boolean.FALSE, // no drop
                null, //
                null, //
                String.format(
                        "CREATE ALIAS IF NOT EXISTS NXFT_INIT FOR \"%s.init\"; "
                                + "CALL NXFT_INIT()", h2Fulltext)));

        statements.add(new ConditionalStatement(
                false, // late
                Boolean.FALSE, // no drop
                null, //
                null, //
                String.format(
                        "CALL NXFT_CREATE_INDEX('PUBLIC', '%s', ('%s', '%s'), '%s')",
                        ft.getName(), ftst.getPhysicalName(),
                        ftbt.getPhysicalName(), fulltextAnalyzer)));

        return statements;
    }

    private ConditionalStatement makeFunction(String functionName,
            String methodName) {
        return new ConditionalStatement( //
                true, // early
                Boolean.TRUE, // always drop
                null, //
                String.format("DROP ALIAS IF EXISTS %s", functionName), //
                String.format("CREATE ALIAS %s FOR \"%s.%s\"", functionName,
                        h2Functions, methodName));
    }

}
