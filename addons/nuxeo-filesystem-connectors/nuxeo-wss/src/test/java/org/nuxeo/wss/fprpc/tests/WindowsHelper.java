/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.wss.fprpc.tests;

public class WindowsHelper {

    private static final String CR = "\r";

    private static final String LF = "\n";

    private static final String CRLF = "\r\n";

    public static String[] splitLines(String result) {
        String[] lines = result.replace(CRLF, LF).replace(CR, LF).split(LF);
        return lines;
    }

}
