/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.storage.sql.jdbc.dialect;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.nuxeo.ecm.core.storage.sql.Activator;
import org.nuxeo.ecm.core.storage.sql.jdbc.JDBCLogger;

/**
 * A SQL statement and some optional tags that condition execution.
 */
public class SQLStatement {

    // for derby...
    public static final String DIALECT_WITH_NO_SEMICOLON = "noSemicolon";

    /** Category pseudo-tag */
    public static final String CATEGORY = "#CATEGORY:";

    /**
     * Tags that may condition execution of the statement.
     */
    public static class Tag {

        /**
         * Tag for a SELECT statement whose number of rows must be counted. Var "emptyResult" is set accordingly.
         */
        public static final String TAG_TEST = "#TEST:";

        /**
         * Tag to only execute statement if a var is true. Var may be preceded by ! inverse the test.
         */
        public static final String TAG_IF = "#IF:";

        /**
         * Tag to define a stored procedure / function / type / trigger. Followed by its name. Use by
         * {@link Dialect#checkStoredProcedure}.
         */
        public static final String TAG_PROC = "#PROC:";

        /**
         * Tag to set a var to true if the result if the statement is empty.
         */
        public static final String TAG_SET_IF_EMPTY = "#SET_IF_EMPTY:";

        /**
         * Tag to set a var to true if the result if the statement is not empty.
         */
        public static final String TAG_SET_IF_NOT_EMPTY = "#SET_IF_NOT_EMPTY:";

        public static final String VAR_EMPTY_RESULT = "emptyResult";

        /** The tag key. */
        public final String key;

        /**
         * The value behind a tag, used for {@link #TAG_IF}, {@link #TAG_PROC}, {@link #TAG_SET_IF_EMPTY},
         * {@link #TAG_SET_IF_NOT_EMPTY}
         */
        public final String value;

        public Tag(String key, String value) {
            this.key = key;
            this.value = value;
        }
    }

    /**
     * Collects a list of strings.
     *
     * @since 6.0-HF24, 7.10-HF01, 8.1
     */
    public static class ListCollector {

        private final List<String> list = new ArrayList<>();

        /** Collects one string. */
        public void add(String string) {
            list.add(string);
        }

        /** Collects several strings. */
        public void addAll(List<String> strings) {
            list.addAll(strings);
        }

        /** Gets the collected strings. */
        public List<String> getStrings() {
            return list;
        }
    }

    /** SQL statement */
    public final String sql;

    /** Tags on the statement */
    public final List<Tag> tags;

    public SQLStatement(String sql, List<Tag> tags) {
        this.sql = sql;
        this.tags = tags == null ? Collections.<Tag> emptyList() : tags;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append("SQLStatement(");
        for (Tag tag : tags) {
            buf.append(tag.key);
            String value = tag.value;
            if (value != null) {
                buf.append(' ');
                buf.append(value);
            }
            buf.append(", ");
        }
        buf.append(sql);
        buf.append(')');
        return buf.toString();
    }

    /**
     * Reads SQL statements from a text file.
     * <p>
     * Statements have a category, and optional tags (that may condition execution).
     *
     * <pre>
     *   #CATEGORY: mycat
     *   #TEST:
     *   SELECT foo
     *     from bar;
     * </pre>
     *
     * <pre>
     *   #CATEGORY: mycat
     *   #IF: emptyResult
     *   #IF: somethingEnabled
     *   INSERT INTO ...;
     * </pre>
     *
     * An empty line terminates a statement.
     */
    public static Map<String, List<SQLStatement>> read(String filename, Map<String, List<SQLStatement>> statements)
            throws IOException {
        return read(filename, statements, false);
    }

    public static Map<String, List<SQLStatement>> read(String filename, Map<String, List<SQLStatement>> statements,
            boolean allDDL) throws IOException {
        InputStream is = Activator.getResourceAsStream(filename);
        if (is == null) {
            throw new IOException("Cannot open: " + filename);
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
        String line;
        String category = null;
        List<Tag> tags = new LinkedList<>();
        try {
            while ((line = reader.readLine()) != null) {
                int colonPos = line.indexOf(':');
                String key = colonPos < 0 ? "" : line.substring(0, colonPos + 1);
                String value = colonPos < 0 ? "" : line.substring(colonPos + 1).trim();
                switch (key) {
                case SQLStatement.CATEGORY:
                    category = value;
                    continue;
                case Tag.TAG_TEST:
                case Tag.TAG_IF:
                case Tag.TAG_PROC:
                case Tag.TAG_SET_IF_EMPTY:
                case Tag.TAG_SET_IF_NOT_EMPTY:
                    if (value.length() == 0) {
                        value = null;
                    }
                    tags.add(new Tag(key, value));
                    continue;
                }
                if (line.startsWith("#")) {
                    continue;
                }
                StringBuilder buf = new StringBuilder();
                boolean read = false;
                while (true) {
                    if (read) {
                        line = reader.readLine();
                    } else {
                        read = true;
                    }
                    if (line == null || line.trim().equals("")) {
                        if (buf.length() == 0) {
                            break;
                        }
                        String sql = buf.toString().trim();
                        SQLStatement statement = new SQLStatement(sql, tags);
                        List<SQLStatement> catStatements = statements.get(category);
                        if (catStatements == null) {
                            statements.put(category, catStatements = new LinkedList<SQLStatement>());
                        }
                        catStatements.add(statement);
                        break;
                    } else if (line.startsWith("#")) {
                        continue;
                    } else {
                        buf.append(line);
                        buf.append('\n');
                    }
                }
                tags = new LinkedList<>();
                if (line == null) {
                    break;
                }
            }
        } finally {
            reader.close();
        }
        return statements;
    }

    protected static String replaceVars(String sql, Map<String, Serializable> properties) {
        if (properties != null) {
            for (Entry<String, Serializable> en : properties.entrySet()) {
                String key = "${" + en.getKey() + "}";
                String value = String.valueOf(en.getValue());
                sql = sql.replaceAll(Pattern.quote(key), Matcher.quoteReplacement(value));
            }
        }
        return sql;
    }

    /**
     * Executes a list of SQL statements, following the tags.
     */
    public static void execute(List<SQLStatement> statements, String ddlMode, Map<String, Serializable> properties,
            Dialect dialect, Connection connection, JDBCLogger logger, ListCollector ddlCollector) throws SQLException {
        try (Statement st = connection.createStatement()) {
            STATEMENT: //
            for (SQLStatement statement : statements) {
                boolean test = false;
                String proc = null;
                Set<String> setIfEmpty = new HashSet<>();
                Set<String> setIfNotEmpty = new HashSet<>();
                for (Tag tag : statement.tags) {
                    switch (tag.key) {
                    case Tag.TAG_TEST:
                        test = true;
                        break;
                    case Tag.TAG_PROC:
                        proc = tag.value;
                        break;
                    case Tag.TAG_IF:
                        String expr = tag.value;
                        boolean res = false;
                        for (String key : expr.split(" OR: ")) {
                            boolean neg = key.startsWith("!");
                            if (neg) {
                                key = key.substring(1).trim();
                            }
                            Serializable value = properties.get(key);
                            if (value == null) {
                                logger.log("Defaulting to false: " + key);
                                value = Boolean.FALSE;
                            }
                            if (!(value instanceof Boolean)) {
                                logger.error("Not a boolean condition: " + key);
                                continue STATEMENT;
                            }
                            if (((Boolean) value).booleanValue() != neg) {
                                res = true;
                                break;
                            }
                        }
                        if (!res) {
                            continue STATEMENT;
                        }
                        break;
                    case Tag.TAG_SET_IF_EMPTY:
                        setIfEmpty.add(tag.value);
                        break;
                    case Tag.TAG_SET_IF_NOT_EMPTY:
                        setIfNotEmpty.add(tag.value);
                        break;
                    }
                }
                String sql = statement.sql;
                sql = replaceVars(sql, properties);
                if (sql.startsWith("LOG.DEBUG")) {
                    String msg = sql.substring("LOG.DEBUG".length()).trim();
                    logger.log(msg);
                    continue;
                } else if (sql.startsWith("LOG.INFO")) {
                    String msg = sql.substring("LOG.INFO".length()).trim();
                    logger.info(msg);
                    continue;
                } else if (sql.startsWith("LOG.ERROR")) {
                    String msg = sql.substring("LOG.ERROR".length()).trim();
                    logger.error(msg);
                    continue;
                } else if (sql.startsWith("LOG.FATAL")) {
                    String msg = sql.substring("LOG.FATAL".length()).trim();
                    logger.error(msg);
                    throw new SQLException("Fatal error: " + msg);
                }

                if (sql.endsWith(";") && properties.containsKey(DIALECT_WITH_NO_SEMICOLON)) {
                    // derby at least doesn't allow a terminating semicolon
                    sql = sql.substring(0, sql.length() - 1);
                }

                try {
                    if (test) {
                        logger.log(sql.replace("\n", "\n    ")); // indented
                        try (ResultSet rs = st.executeQuery(sql)) {
                            boolean empty = !rs.next();
                            properties.put(Tag.VAR_EMPTY_RESULT, Boolean.valueOf(empty));
                            logger.log("  -> emptyResult = " + empty);
                            if (empty) {
                                for (String prop : setIfEmpty) {
                                    properties.put(prop, Boolean.TRUE);
                                    logger.log("  -> " + prop + " = true");
                                }
                            } else {
                                for (String prop : setIfNotEmpty) {
                                    properties.put(prop, Boolean.TRUE);
                                    logger.log("  -> " + prop + " = true");
                                }
                            }
                        }
                    } else if (proc != null) {
                        ddlCollector.addAll(
                                dialect.checkStoredProcedure(proc, sql, ddlMode, connection, logger, properties));
                    } else if (ddlCollector != null) {
                        ddlCollector.add(sql);
                    } else {
                        // upgrade stuff, execute immediately
                        logger.log(sql.replace("\n", "\n    ")); // indented
                        st.execute(sql);
                    }
                } catch (SQLException e) {
                    throw new SQLException("Error executing: " + sql + " : " + e.getMessage(), e);
                }
            }
        }
    }

}
