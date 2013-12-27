/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
    public static boolean isInTree(Serializable id, Serializable baseId)
            throws SQLException {
        Connection conn = DriverManager.getConnection("jdbc:default:connection");
        try {
            return isInTree(conn, id, baseId);
        } finally {
            conn.close();
        }
    }

    /**
     * Checks if an id is a (strict) descendant of a given base id.
     *
     * @param conn the connection to the database
     * @param id the id to check for
     * @param baseId the base id
     */
    public static boolean isInTree(Connection conn, Serializable id,
            Serializable baseId) throws SQLException {
        if (baseId == null || id == null || baseId.equals(id)) {
            // containment check is strict
            return false;
        }
        PreparedStatement ps = null;
        try {
            ps = conn.prepareStatement("SELECT PARENTID, ISPROPERTY FROM HIERARCHY WHERE ID = ?");
            do {
                ps.setObject(1, id);
                ResultSet rs = ps.executeQuery();
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
                rs.close();
                if (isProperty) {
                    // a complex property is never in-tree
                    return false;
                }
                if (baseId.equals(id)) {
                    // found a match
                    return true;
                }
            } while (id != null);
            // got to the root
            return false;
        } finally {
            if (ps != null) {
                ps.close();
            }
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
     */
    public static boolean isAccessAllowed(Serializable id,
            Set<String> principals, Set<String> permissions)
            throws SQLException {
        Connection conn = DriverManager.getConnection("jdbc:default:connection");
        try {
            return isAccessAllowed(conn, id, principals, permissions);
        } finally {
            conn.close();
        }
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
     */
    public static boolean isAccessAllowed(Connection conn, Serializable id,
            Set<String> principals, Set<String> permissions)
            throws SQLException {
        if (isLogEnabled()) {
            logDebug("isAccessAllowed " + id + " " + principals + " "
                    + permissions);
        }
        PreparedStatement ps1 = null;
        PreparedStatement ps2 = null;
        PreparedStatement ps3 = null;
        try {
            ps1 = conn.prepareStatement( //
            "SELECT \"GRANT\", \"PERMISSION\", \"USER\" FROM \"ACLS\" "
                    + "WHERE ID = ? ORDER BY POS");
            ps2 = conn.prepareStatement("SELECT PARENTID FROM HIERARCHY WHERE ID = ?");
            boolean first = true;
            do {
                /*
                 * Check permissions at this level.
                 */
                ps1.setObject(1, id);
                ResultSet rs = ps1.executeQuery();
                while (rs.next()) {
                    boolean grant = rs.getShort(1) != 0;
                    String permission = rs.getString(2);
                    String user = rs.getString(3);
                    if (isLogEnabled()) {
                        logDebug(" -> " + user + " " + permission + " " + grant);
                    }
                    if (principals.contains(user)
                            && permissions.contains(permission)) {
                        if (isLogEnabled()) {
                            logDebug(" => " + grant);
                        }
                        return grant;
                    }
                }
                /*
                 * Nothing conclusive found, repeat on the parent.
                 */
                ps2.setObject(1, id);
                rs = ps2.executeQuery();
                Serializable newId;
                if (rs.next()) {
                    newId = (Serializable) rs.getObject(1);
                    if (rs.wasNull()) {
                        newId = null;
                    }
                } else {
                    // no such id
                    newId = null;
                }
                if (first && newId == null) {
                    // there is no parent for the first level
                    // we may have a version on our hands, find the live doc
                    ps3 = conn.prepareStatement("SELECT VERSIONABLEID FROM VERSIONS WHERE ID = ?");
                    ps3.setObject(1, id);
                    rs = ps3.executeQuery();
                    if (rs.next()) {
                        newId = (Serializable) rs.getObject(1);
                        if (rs.wasNull()) {
                            newId = null;
                        }
                    } else {
                        // no such id
                        newId = null;
                    }
                }
                first = false;
                id = newId;
            } while (id != null);
            /*
             * We reached the root, deny access.
             */
            if (isLogEnabled()) {
                logDebug(" => false (root)");
            }
            return false;
        } finally {
            if (ps1 != null) {
                ps1.close();
            }
            if (ps2 != null) {
                ps2.close();
            }
            if (ps3 != null) {
                ps3.close();
            }
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
            + "car donc est il ils je la le les mais ni nous or ou pour tu un une vous "
            + "www com net org";

    private static final Set<String> stopWords = new HashSet<String>(split(
            STOPWORDS, ' '));

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
