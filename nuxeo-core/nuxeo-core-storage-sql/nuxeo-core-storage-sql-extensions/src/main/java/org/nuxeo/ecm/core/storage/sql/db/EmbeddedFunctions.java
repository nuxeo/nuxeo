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

package org.nuxeo.ecm.core.storage.sql.db;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Functions used as stored procedures for Derby and H2.
 *
 * @author Florent Guillaume
 */
public class EmbeddedFunctions {

    protected static final String READ = "Read";

    protected static final String READ_VERSION = "ReadVersion";

    // for debug
    private static boolean isLogEnabled() {
        return false;
        // return log.isTraceEnabled();
    }

    // for debug
    private static void logDebug(String message) {
        // log.trace(message);
    }

    /**
     * Checks if an id is a (strict) descendant of a given base id.
     *
     * @param id the id to check for
     * @param baseId the base id
     */
    public static boolean isInTree(Serializable id, Serializable baseId) throws SQLException {
        try (Connection conn = DriverManager.getConnection("jdbc:default:connection")) {
            return isInTree(conn, id, baseId);
        }
    }

    /**
     * Checks if an id is a (strict) descendant of a given base id.
     *
     * @param conn the connection to the database
     * @param id the id to check for
     * @param baseId the base id
     */
    public static boolean isInTree(Connection conn, Serializable id, Serializable baseId) throws SQLException {
        if (baseId == null || id == null || baseId.equals(id)) {
            // containment check is strict
            return false;
        }
        try (PreparedStatement ps = conn.prepareStatement("SELECT PARENTID, ISPROPERTY FROM HIERARCHY WHERE ID = ?")) {
            do {
                ps.setObject(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        // no such id
                        return false;
                    }
                    if (id instanceof String) {
                        id = rs.getString(1);
                    } else {
                        id = Long.valueOf(rs.getLong(1));
                    }
                    if (rs.wasNull()) {
                        id = null;
                    }
                    boolean isProperty = rs.getBoolean(2);
                    if (isProperty) {
                        // a complex property is never in-tree
                        return false;
                    }
                }
                if (baseId.equals(id)) {
                    // found a match
                    return true;
                }
            } while (id != null);
            // got to the root
            return false;
        }
    }

    /**
     * Checks if access to a document is allowed.
     * <p>
     * This implements in SQL the ACL-based security policy logic.
     *
     * @param id the id of the document
     * @param principals the allowed identities
     * @param permissions the allowed permissions
     * @deprecated since 11.3, unused
     */
    @Deprecated
    public static boolean isAccessAllowed(Serializable id, Set<String> principals, Set<String> permissions)
            throws SQLException {
        try (Connection conn = DriverManager.getConnection("jdbc:default:connection")) {
            return isAccessAllowed(conn, id, principals, permissions);
        }
    }

    /**
     * @deprecated since 11.3, use other signature
     */
    @Deprecated
    public static boolean isAccessAllowed(Connection conn, Serializable id, Set<String> principals,
            Set<String> permissions) throws SQLException {
        return isAccessAllowed(conn, id, principals, permissions, true, true);
    }

    /**
     * Checks if access to a document is allowed.
     * <p>
     * This implements in SQL the ACL-based security policy logic.
     *
     * @param conn the database connection
     * @param id the id of the document
     * @param principals the allowed identities
     * @param permissions the allowed permissions
     * @param disableVersionACL whether ACLs on a version are disabled
     * @param disableReadVersionPermission whether the ReadVersion permission is disabled
     * @since 11.3
     */
    public static boolean isAccessAllowed(Connection conn, Serializable id, Set<String> principals,
            Set<String> permissions, boolean disableVersionACL, boolean disableReadVersionPermission)
            throws SQLException {
        try (PreparedStatement psAcl = conn.prepareStatement(
                "SELECT \"GRANT\", \"PERMISSION\", \"USER\" FROM \"ACLS\" WHERE ID = ? AND (STATUS IS NULL OR STATUS = 1) ORDER BY POS");
                PreparedStatement psHier = conn.prepareStatement(
                        "SELECT PARENTID, ISVERSION FROM HIERARCHY WHERE ID = ?");
                PreparedStatement psVer = conn.prepareStatement("SELECT VERSIONABLEID FROM VERSIONS WHERE ID = ?")) {
            boolean replaceReadVersionPermission = false;
            RowInfo rowInfo = null; // info about the row for the current id
            if (disableVersionACL) {
                // if it's a version, ignore its ACL and find the live doc
                rowInfo = getRowInfo(psHier, psVer, id);
                if (rowInfo.isVersion) {
                    replaceReadVersionPermission = !disableReadVersionPermission;
                    id = rowInfo.versionableId;
                    if (id == null) {
                        return false;
                    }
                    rowInfo = null;
                }
            }
            do {
                // check permissions at this level
                Boolean access = getAccess(psAcl, id, principals, permissions, replaceReadVersionPermission);
                if (access != null) {
                    return access;
                }
                // nothing conclusive found, repeat on the parent
                if (rowInfo == null) {
                    rowInfo = getRowInfo(psHier, psVer, id);
                }
                if (rowInfo.isVersion) {
                    replaceReadVersionPermission = !disableReadVersionPermission;
                    id = rowInfo.versionableId;
                } else {
                    id = rowInfo.parentId;
                }
                rowInfo = null;
            } while (id != null);
            // we've reached the root or a placeless document, deny access
            return false;
        }
    }

    protected static class RowInfo {

        public Serializable parentId;

        public boolean isVersion;

        public Serializable versionableId;
    }

    protected static RowInfo getRowInfo(PreparedStatement psHier, PreparedStatement psVer, Serializable id)
            throws SQLException {
        psHier.setObject(1, id);
        try (ResultSet rs = psHier.executeQuery()) {
            RowInfo rowInfo = new RowInfo();
            if (rs.next()) {
                rowInfo.parentId = (Serializable) rs.getObject(1);
                rowInfo.isVersion = rs.getBoolean(2);
                if (rowInfo.isVersion) {
                    psVer.setObject(1, id);
                    try (ResultSet rs2 = psVer.executeQuery()) {
                        if (rs2.next()) {
                            rowInfo.versionableId = (Serializable) rs2.getObject(1);
                        }
                    }
                }
            }
            return rowInfo;
        }
    }

    protected static Boolean getAccess(PreparedStatement psAcl, Serializable id, Set<String> principals,
            Set<String> permissions, boolean replaceReadVersionPermission) throws SQLException {
        psAcl.setObject(1, id);
        try (ResultSet rs = psAcl.executeQuery()) {
            while (rs.next()) {
                boolean grant = rs.getShort(1) != 0;
                String permission = rs.getString(2);
                String user = rs.getString(3);
                if (replaceReadVersionPermission && READ_VERSION.equals(permission)) {
                    permission = READ;
                }
                if (principals.contains(user) && permissions.contains(permission)) {
                    return grant;
                }
            }
            return null; // no grant nor deny -- NOSONAR
        }
    }

    /**
     * Extracts the words from a string for simple fulltext indexing.
     *
     * @param string1 the first string
     * @param string2 the second string
     * @return a string with extracted words
     */
    public static String parseFullText(String string1, String string2) {
        Set<String> set = new HashSet<String>();
        set.addAll(parseFullText(string1));
        set.addAll(parseFullText(string2));
        List<String> words = new ArrayList<String>(set);
        Collections.sort(words);
        return join(words, ' ');
    }

    protected static Set<String> parseFullText(String string) {
        if (string == null) {
            return Collections.emptySet();
        }
        Set<String> set = new HashSet<String>();
        for (String word : wordPattern.split(string)) {
            String w = parseWord(word);
            if (w != null) {
                set.add(w);
            }
        }
        return set;
    }

    /**
     * Checks if the passed query expression matches the fulltext.
     *
     * @param fulltext the fulltext, space-separated words
     * @param query a list of space-separated words
     * @return {@code true} if all the words are in the fulltext
     */
    protected static boolean matchesFullText(String fulltext, String query) {
        if (fulltext == null || query == null) {
            return false;
        }
        Set<String> words = split(query.toLowerCase(), ' ');
        Set<String> filtered = new HashSet<String>();
        for (String word : words) {
            if (!wordPattern.matcher(word).matches()) {
                filtered.add(word);
            }
        }
        words = filtered;
        if (words.isEmpty()) {
            return false;
        }
        Set<String> fulltextWords = split(fulltext.toLowerCase(), ' ');
        for (String word : words) {
            if (word.endsWith("*") || word.endsWith("%")) {
                // prefix match
                String prefix = word.substring(0, word.length() - 2);
                boolean match = false;
                for (String candidate : fulltextWords) {
                    if (candidate.startsWith(prefix)) {
                        match = true;
                        break;
                    }
                }
                if (!match) {
                    return false;
                }
            } else {
                if (!fulltextWords.contains(word)) {
                    return false;
                }
            }
        }
        return true;
    }

    // ----- simple parsing, don't try to be exhaustive -----

    private static final Pattern wordPattern = Pattern.compile("[\\s\\p{Punct}]+");

    private static final String UNACCENTED = "aaaaaaaceeeeiiii\u00f0nooooo\u00f7ouuuuy\u00fey";

    private static final String STOPWORDS = "a an are and as at be by for from how "
            + "i in is it of on or that the this to was what when where who will with "
            + "car donc est il ils je la le les mais ni nous or ou pour tu un une vous " + "www com net org";

    private static final Set<String> stopWords = new HashSet<String>(split(STOPWORDS, ' '));

    public static final String parseWord(String string) {
        int len = string.length();
        if (len < 3) {
            return null;
        }
        StringBuilder buf = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            char c = Character.toLowerCase(string.charAt(i));
            if (c == '\u00e6') {
                buf.append("ae");
            } else if (c >= '\u00e0' && c <= '\u00ff') {
                buf.append(UNACCENTED.charAt((c) - 0xe0));
            } else if (c == '\u0153') {
                buf.append("oe");
            } else {
                buf.append(c);
            }
        }
        // simple heuristic to remove plurals
        int l = buf.length();
        if (l > 3 && buf.charAt(l - 1) == 's') {
            buf.setLength(l - 1);
        }
        String word = buf.toString();
        if (stopWords.contains(word)) {
            return null;
        }
        return word;
    }

    // ----- utility functions -----

    public static Set<String> split(String string) {
        return split(string, '|');
    }

    public static Set<String> split(String string, char sep) {
        int len = string.length();
        if (len == 0) {
            return Collections.emptySet();
        }
        int end = string.indexOf(sep);
        if (end == -1) {
            return Collections.singleton(string);
        }
        Set<String> set = new HashSet<String>();
        int start = 0;
        do {
            String segment = string.substring(start, end);
            set.add(segment);
            start = end + 1;
            end = string.indexOf(sep, start);
        } while (end != -1);
        if (start < len) {
            set.add(string.substring(start));
        } else {
            set.add("");
        }
        return set;
    }

    private static final String join(Collection<String> strings, char sep) {
        if (strings == null || strings.isEmpty()) {
            return "";
        }
        int size = 0;
        for (String word : strings) {
            size += word.length() + 1;
        }
        StringBuilder buf = new StringBuilder(size);
        for (String word : strings) {
            buf.append(word);
            buf.append(sep);
        }
        buf.setLength(size - 1);
        return buf.toString();
    }

}
