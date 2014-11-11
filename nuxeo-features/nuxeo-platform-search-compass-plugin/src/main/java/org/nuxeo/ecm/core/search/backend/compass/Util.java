/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.core.search.backend.compass;

/**
 * A static class for various utilities that need
 * to be accessed from different classes and don't depend
 * on Compass or Nuxeo specific objects.
 *
 * @author gracinet
 */
public final class Util {

    public static final String EMPTY_MARKER = "_empty_marker";
    public static final String NULL_MARKER = "_null_marker";

    public static final String COMPASS_FULLTEXT = "all";
    public static final String SORTABLE_FIELD_SUFFIX = "::sort";

    private static final String ESCAPE = "[_]+(empty_marker|null_marker)";
    private static final String UNESCAPE = "_([_]+(empty_marker|null_marker))";

    // Utility class.
    private Util() {
    }

    /**
     * Escape system for empty marker.
     * Works by shifting the number of leading underscores.
     *
     * @param value
     * @return the escaped value
     */
    public static String escapeSpecialMarkers(String value) {
        return value.replaceAll(ESCAPE, "_$0");
    }

    /**
     * Escape system for empty marker.
     * Works by shifting the number of leading underscores.
     *
     * @param value
     * @return the original value
     */
    public static String unescapeSpecialMarkers(String value) {
        return value.replaceAll(UNESCAPE, "$1");
    }

}
