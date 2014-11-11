/*
 * (C) Copyright 2006-2007 Nuxeo SAS <http://nuxeo.com> and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jean-Marc Orliaguet, Chalmers
 *
 * $Id$
 */

package org.nuxeo.theme;

public final class Utils {

    private Utils() {
        // This class is not supposed to be instanciated.
    }

    public static boolean contains(final String[] array, final String value) {
        for (String s : array) {
            if (s.equals(value)) {
                return true;
            }
        }
        return false;
    }

    public static String cleanUp(String text) {
        return text.replaceAll("\n", " ").replaceAll("\\t+", " ").replaceAll(
                "\\s+", " ").trim();
    }

}
