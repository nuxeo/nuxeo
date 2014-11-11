/*
 * Copyright 2004-2008 H2 Group. Multiple-Licensed under the H2 License,
 * Version 1.0, and under the Eclipse Public License, Version 1.0
 * (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 * Contributor: Florent Guillaume
 */
package org.nuxeo.ecm.core.storage.sql.db;

import java.io.IOException;
import java.io.Reader;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Hit;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.TermQuery;
import org.h2.api.CloseListener;
import org.h2.message.Message;
import org.h2.store.fs.FileSystem;
import org.h2.tools.SimpleResultSet;
import org.h2.util.IOUtils;
import org.h2.util.StringUtils;
import org.h2.value.DataType;

/**
 * An optimized Lucene-based fulltext indexing trigger and search.
 *
 * @author H2 Group
 * @author Florent Guillaume
 */
public class H2Fulltext {

    private static final Map<String, IndexWriter> indexWriters = new ConcurrentHashMap<String, IndexWriter>();

    private static final String FT_SCHEMA = "NXFT";
    private static final String FT_TABLE = "NXFT.INDEXES";

    private static final String PREFIX = "NXFT_";

    private static final String FIELD_SCHEMA = "SCHEMA";
    private static final String FIELD_TABLE = "TABLE";
    private static final String FIELD_KEY = "KEY";
    private static final String FIELD_TEXT = "TEXT";

    // Utility class.
    private H2Fulltext() {
    }

    /**
     * Initializes fulltext search functionality for this database. This adds
     * the following Java functions to the database:
     * <ul>
     * <li>NXFT_CREATE_INDEX(schemaString, tableString, columnListString)</li>
     * <li>NXFT_REINDEX()</li>
     * <li>NXFT_DROP_ALL()</li>
     * <li>NXFT_SEARCH(queryString, limitInt, offsetInt): result set</li>
     * </ul>
     * It also adds a schema NXFT to the database where bookkeeping information
     * is stored. This function may be called from a Java application, or by
     * using the SQL statements:
     *
     * <pre>
     *  CREATE ALIAS IF NOT EXISTS NXFT_INIT FOR
     *      &quot;org.nuxeo.ecm.core.storage.sql.db.H2Fulltext.init&quot;;
     *  CALL NXFT_INIT();
     * </pre>
     *
     * @param conn
     */
    public static void init(Connection conn) throws SQLException {
        Statement st = conn.createStatement();
        st.execute("CREATE SCHEMA IF NOT EXISTS " + FT_SCHEMA);
        st.execute("CREATE TABLE IF NOT EXISTS " + FT_TABLE +
                "(SCHEMA VARCHAR, TABLE VARCHAR, COLUMNS VARCHAR, " +
                "ANALYZER VARCHAR, PRIMARY KEY(SCHEMA, TABLE))");
        String className = H2Fulltext.class.getName();
        st.execute("CREATE ALIAS IF NOT EXISTS " + PREFIX +
                "CREATE_INDEX FOR \"" + className + ".createIndex\"");
        st.execute("CREATE ALIAS IF NOT EXISTS " + PREFIX + "REINDEX FOR \"" +
                className + ".reindex\"");
        st.execute("CREATE ALIAS IF NOT EXISTS " + PREFIX + "DROP_ALL FOR \"" +
                className + ".dropAll\"");
        st.execute("CREATE ALIAS IF NOT EXISTS " + PREFIX + "SEARCH FOR \"" +
                className + ".search\"");
    }

    // ----- static methods called directly to initialize fulltext -----

    /**
     * Creates a fulltext index for a table and column list.
     * <p>
     * Each table may only have one index at any time. If the index already
     * exists, nothing is done, otherwise the index is created and populated
     * from existing data.
     * <p>
     * Usually called through:
     *
     * <pre>
     *   CALL NXFT_CREATE_INDEX('myschema', 'mytable', ('col1', 'col2'), 'lucene.analyzer');
     * </pre>
     *
     * @param conn the connection
     * @param schema the schema name of the table
     * @param table the table name
     * @param columns the column list
     * @param analyzer the Lucene fulltext analyzer class
     */
    public static void createIndex(Connection conn, String schema,
            String table, String columns, String analyzer) throws SQLException {
        columns = columns.replace("(", "").replace(")", "").replace(" ", "");
        PreparedStatement ps = conn.prepareStatement("DELETE FROM " + FT_TABLE +
                " WHERE SCHEMA = ? AND TABLE = ?");
        ps.setString(1, schema);
        ps.setString(2, table);
        ps.execute();
        ps = conn.prepareStatement("INSERT INTO " + FT_TABLE +
                "(SCHEMA, TABLE, COLUMNS, ANALYZER) VALUES(?, ?, ?, ?)");
        ps.setString(1, schema);
        ps.setString(2, table);
        ps.setString(3, columns);
        ps.setString(4, analyzer);
        ps.execute();
        ps.close();
        createTrigger(conn, schema, table);
        indexExistingRows(conn, schema, table);
    }

    /**
     * Re-creates the fulltext index for this database.
     */
    public static void reindex(Connection conn) throws SQLException {
        removeAllTriggers(conn);
        removeIndexFiles(conn);
        Statement st = conn.createStatement();
        ResultSet rs = st.executeQuery("SELECT * FROM " + FT_TABLE);
        while (rs.next()) {
            String schema = rs.getString("SCHEMA");
            String table = rs.getString("TABLE");
            createTrigger(conn, schema, table);
            indexExistingRows(conn, schema, table);
        }
        st.close();
    }

    private static void indexExistingRows(Connection conn, String schema,
            String table) throws SQLException {
        Trigger trigger = new Trigger();
        trigger.init(conn, schema, null, table, false, Trigger.INSERT);
        Statement st = conn.createStatement();
        ResultSet rs = st.executeQuery("SELECT * FROM " +
                StringUtils.quoteIdentifier(schema) + '.' +
                StringUtils.quoteIdentifier(table));
        int n = rs.getMetaData().getColumnCount();
        while (rs.next()) {
            Object[] row = new Object[n];
            for (int i = 0; i < n; i++) {
                row[i] = rs.getObject(i + 1);
            }
            trigger.fire(conn, null, row);
        }
        st.close();
    }

    private static void createTrigger(Connection conn, String schema,
            String table) throws SQLException {
        Statement st = conn.createStatement();
        schema = StringUtils.quoteIdentifier(schema);
        String trigger = schema + '.' +
                StringUtils.quoteIdentifier(PREFIX + table);
        st.execute("DROP TRIGGER IF EXISTS " + trigger);
        st.execute(String.format("CREATE TRIGGER IF NOT EXISTS %s "
                + "AFTER INSERT, UPDATE, DELETE ON %s.%s "
                + "FOR EACH ROW CALL \"%s\"", trigger, schema,
                StringUtils.quoteIdentifier(table),
                H2Fulltext.Trigger.class.getName()));
        st.close();
    }

    private static void removeAllTriggers(Connection conn) throws SQLException {
        Statement st = conn.createStatement();
        ResultSet rs = st.executeQuery("SELECT * FROM INFORMATION_SCHEMA.TRIGGERS");
        Statement st2 = conn.createStatement();
        while (rs.next()) {
            String trigger = rs.getString("TRIGGER_NAME");
            if (trigger.startsWith(PREFIX)) {
                st2.execute("DROP TRIGGER " +
                        StringUtils.quoteIdentifier(rs.getString("TRIGGER_SCHEMA")) +
                        "." + trigger);
            }
        }
        st.close();
        st2.close();
    }

    /**
     * Drops all fulltext indexes from the database.
     */
    public static void dropAll(Connection conn) throws SQLException {
        Statement st = conn.createStatement();
        st.execute("DROP SCHEMA IF EXISTS " + FT_SCHEMA);
        st.close();
        removeAllTriggers(conn);
        removeIndexFiles(conn);
    }

    /**
     * Searches from the given full text index. The returned result set has a
     * single ID column which holds the keys for the matching rows.
     * <p>
     * Usually called through:
     *
     * <pre>
     *   SELECT * FROM NXFT_SEARCH(schema, table, 'text');
     * </pre>
     *
     * @param conn the connection
     * @param text the search query
     * @return the result set
     */
    @SuppressWarnings("unchecked")
    public static ResultSet search(Connection conn, String schema,
            String table, String text) throws SQLException {
        DatabaseMetaData meta = conn.getMetaData();
        int type = getPrimaryKeyType(meta, schema, table);
        SimpleResultSet rs = new SimpleResultSet();
        rs.addColumn(FIELD_KEY, type, 0, 0);

        if (meta.getURL().startsWith("jdbc:columnlist:")) {
            // this is just to query the result set columns
            return rs;
        }

        String indexPath = getIndexPath(conn);

        // find analyzer
        PreparedStatement ps = conn.prepareStatement("SELECT ANALYZER FROM " +
                FT_TABLE + " WHERE SCHEMA = ? AND TABLE = ?");
        ps.setString(1, schema);
        ps.setString(2, table);
        ResultSet res = ps.executeQuery();
        if (!res.next()) {
            throw new SQLException("No index for table: " + schema + '.' +
                    table);
        }
        String analyzer = res.getString(1);
        ps.close();

        try {
            BooleanQuery query = new BooleanQuery();
            QueryParser parser = new QueryParser(FIELD_TEXT,
                    getAnalyzer(analyzer));
            query.add(parser.parse(text), BooleanClause.Occur.MUST);
            query.add(new TermQuery(new Term(FIELD_SCHEMA, schema)),
                    BooleanClause.Occur.MUST);
            query.add(new TermQuery(new Term(FIELD_TABLE, table)),
                    BooleanClause.Occur.MUST);

            getIndexWriter(indexPath, analyzer).flush();
            Searcher searcher = new IndexSearcher(indexPath);
            Iterator<Hit> it = searcher.search(query).iterator();
            for (; it.hasNext();) {
                Hit hit = it.next();
                Object key = asObject(hit.get(FIELD_KEY), type);
                rs.addRow(new Object[] { key });
            }
            // TODO keep it open if possible
            searcher.close();
        } catch (Exception e) {
            throw convertException(e);
        }
        return rs;
    }

    private static int getPrimaryKeyType(DatabaseMetaData meta, String schema,
            String table) throws SQLException {
        // find primary key name
        String primaryKeyName = null;
        ResultSet rs = meta.getPrimaryKeys(null, schema, table);
        while (rs.next()) {
            if (primaryKeyName != null) {
                throw new SQLException(
                        "Can only index primary keys on one column for " +
                                schema + '.' + table);
            }
            primaryKeyName = rs.getString("COLUMN_NAME");
        }
        if (primaryKeyName == null) {
            throw new SQLException("No primary key for " + schema + '.' + table);
        }
        rs.close();

        // find primary key type
        rs = meta.getColumns(null, schema, table, primaryKeyName);
        if (!rs.next()) {
            throw new SQLException("Could not find primary key");
        }
        int primaryKeyType = rs.getInt("DATA_TYPE");
        rs.close();

        return primaryKeyType;
    }

    private static Analyzer getAnalyzer(String analyzer) throws SQLException {
        try {
            return (Analyzer) Class.forName(analyzer).newInstance();
        } catch (Exception e) {
            throw new SQLException(e.toString());
        }
    }

    private static String getIndexPath(Connection conn) throws SQLException {
        Statement st = conn.createStatement();
        ResultSet rs = st.executeQuery("CALL DATABASE_PATH()");
        rs.next();
        String path = rs.getString(1);
        if (path == null) {
            throw new SQLException(
                    "Fulltext search for in-memory databases is not supported.");
        }
        st.close();
        return path + ".lucene";
    }

    private static IndexWriter getIndexWriter(String indexPath, String analyzer)
            throws SQLException {
        IndexWriter indexWriter = indexWriters.get(indexPath);
        if (indexWriter == null) {
            synchronized (indexWriters) {
                if (!indexWriters.containsKey(indexPath)) {
                    try {
                        boolean recreate = !IndexReader.indexExists(indexPath);
                        indexWriter = new IndexWriter(indexPath,
                                getAnalyzer(analyzer), recreate);
                    } catch (IOException e) {
                        throw convertException(e);
                    }
                    indexWriters.put(indexPath, indexWriter);
                }
            }
        }
        return indexWriter;
    }

    private static void removeIndexFiles(Connection conn) throws SQLException {
        String path = getIndexPath(conn);
        IndexWriter index = indexWriters.remove(path);
        if (index != null) {
            try {
                index.flush();
                index.close();
            } catch (IOException e) {
                throw convertException(e);
            }
        }
        FileSystem.getInstance(path).deleteRecursive(path);
    }

    private static SQLException convertException(Exception e) {
        SQLException e2 = new SQLException("Error while indexing document");
        e2.initCause(e);
        return e2;
    }

    protected static String asString(Object data, int type) throws SQLException {
        if (data == null) {
            return "";
        }
        switch (type) {
        case Types.BIT:
        case DataType.TYPE_BOOLEAN:
        case Types.INTEGER:
        case Types.BIGINT:
        case Types.DECIMAL:
        case Types.DOUBLE:
        case Types.FLOAT:
        case Types.NUMERIC:
        case Types.REAL:
        case Types.SMALLINT:
        case Types.TINYINT:
        case Types.DATE:
        case Types.TIME:
        case Types.TIMESTAMP:
        case Types.LONGVARCHAR:
        case Types.CHAR:
        case Types.VARCHAR:
            return data.toString();
        case Types.CLOB:
            try {
                if (data instanceof Clob) {
                    data = ((Clob) data).getCharacterStream();
                }
                return IOUtils.readStringAndClose((Reader) data, -1);
            } catch (IOException e) {
                throw Message.convert(e);
            }
        case Types.VARBINARY:
        case Types.LONGVARBINARY:
        case Types.BINARY:
        case Types.JAVA_OBJECT:
        case Types.OTHER:
        case Types.BLOB:
        case Types.STRUCT:
        case Types.REF:
        case Types.NULL:
        case Types.ARRAY:
        case DataType.TYPE_DATALINK:
        case Types.DISTINCT:
            throw new SQLException("Unsupported column data type: " + type);
        default:
            return "";
        }
    }

    // simple cases only, used for primary key
    private static Object asObject(String string, int type) throws SQLException {
        switch (type) {
        case Types.BIGINT:
            return Long.valueOf(string);
        case Types.INTEGER:
        case Types.SMALLINT:
        case Types.TINYINT:
            return Integer.valueOf(string);
        case Types.LONGVARCHAR:
        case Types.CHAR:
        case Types.VARCHAR:
            return string;
        default:
            throw new SQLException("Unsupport data type for primary key: " +
                    type);
        }
    }

    /**
     * Trigger used to update the lucene index upon row change.
     */
    public static class Trigger implements org.h2.api.Trigger, CloseListener {

        private String schema;

        private String table;

        private String indexPath;

        private IndexWriter indexWriter;

        /** Starting at 0. */
        private int primaryKeyIndex;

        private int primaryKeyType;

        private int[] columnTypes;

        private int[] columnIndices;

        /**
         * Trigger interface: initialization.
         */
        public void init(Connection conn, String schema, String triggerName,
                String table, boolean before, int type) throws SQLException {
            this.schema = schema;
            this.table = table;
            indexPath = getIndexPath(conn);
            DatabaseMetaData meta = conn.getMetaData();

            // find primary key name
            String primaryKeyName = null;
            ResultSet rs = meta.getPrimaryKeys(null, schema, table);
            while (rs.next()) {
                if (primaryKeyName != null) {
                    throw new SQLException(
                            "Can only index primary keys on one column for: " +
                                    schema + '.' + table);
                }
                primaryKeyName = rs.getString("COLUMN_NAME");
            }
            if (primaryKeyName == null) {
                throw new SQLException("No primary key for " + schema + '.' +
                        table);
            }
            rs.close();

            // find primary key type
            rs = meta.getColumns(null, schema, table, primaryKeyName);
            if (!rs.next()) {
                throw new SQLException("No primary key for: " + schema + '.' +
                        table);
            }
            primaryKeyType = rs.getInt("DATA_TYPE");
            primaryKeyIndex = rs.getInt("ORDINAL_POSITION") - 1;
            rs.close();

            // find columns configured for indexing
            PreparedStatement ps = conn.prepareStatement("SELECT COLUMNS, ANALYZER FROM " +
                    FT_TABLE + " WHERE SCHEMA = ? AND TABLE = ?");
            ps.setString(1, schema);
            ps.setString(2, table);
            rs = ps.executeQuery();
            if (!rs.next()) {
                throw new SQLException("No index for table: " + schema + '.' +
                        table);
            }
            String columns = rs.getString(1);
            String analyzer = rs.getString(2);
            ps.close();
            if (columns == null) {
                throw new SQLException("No columns in index for table: " +
                        schema + '.' + table);
            }
            Set<String> columnNames = new HashSet<String>(
                    Arrays.asList(columns.split(",")));

            // find the column's indices
            columnTypes = new int[columnNames.size()];
            columnIndices = new int[columnNames.size()];
            rs = meta.getColumns(null, schema, table, null);
            for (int i = 0; rs.next();) {
                String name = rs.getString("COLUMN_NAME");
                if (!columnNames.contains(name)) {
                    continue;
                }
                columnTypes[i] = rs.getInt("DATA_TYPE");
                columnIndices[i] = rs.getInt("ORDINAL_POSITION") - 1;
                i++;
            }
            rs.close();
            indexWriter = getIndexWriter(indexPath, analyzer);
        }

        /**
         * Trigger interface.
         */
        public void fire(Connection conn, Object[] oldRow, Object[] newRow)
                throws SQLException {
            if (oldRow != null) {
                delete(oldRow);
            }
            if (newRow != null) {
                insert(newRow);
            }
        }

        private void insert(Object[] row) throws SQLException {
            Document doc = new Document();
            doc.add(new Field(FIELD_SCHEMA, schema, Field.Store.NO,
                    Field.Index.UN_TOKENIZED));
            doc.add(new Field(FIELD_TABLE, table, Field.Store.NO,
                    Field.Index.UN_TOKENIZED));
            String key = asString(row[primaryKeyIndex], primaryKeyType);
            doc.add(new Field(FIELD_KEY, key, Field.Store.YES,
                    Field.Index.UN_TOKENIZED));

            StringBuilder buf = new StringBuilder();
            for (int i = 0; i < columnTypes.length; i++) {
                String data = asString(row[columnIndices[i]], columnTypes[i]);
                if (i > 0) {
                    buf.append(' ');
                }
                buf.append(data);
            }
            doc.add(new Field(FIELD_TEXT, buf.toString(), Field.Store.NO,
                    Field.Index.TOKENIZED));

            try {
                indexWriter.addDocument(doc);
            } catch (IOException e) {
                throw convertException(e);
            }
        }

        private void delete(Object[] row) throws SQLException {
            String primaryKey = asString(row[primaryKeyIndex], primaryKeyType);
            try {
                indexWriter.deleteDocuments(new Term(FIELD_KEY, primaryKey));
            } catch (IOException e) {
                throw convertException(e);
            }
        }

        public void close() throws SQLException {
            if (indexWriter != null) {
                try {
                    indexWriter.flush();
                    indexWriter.close();
                    indexWriter = null;
                    indexWriters.remove(indexPath);
                } catch (Exception e) {
                    throw convertException(e);
                }
            }
        }

        public void remove() {
            // ignore
        }
    }

}
