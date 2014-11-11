/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 */
package org.nuxeo.ecm.shell.utils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * 
 */
public class StringUtils {

    public static String join(String[] ar, String delim) {
        if (ar == null || ar.length == 0) {
            return "";
        }
        if (ar.length == 1) {
            return ar[0];
        }
        StringBuilder buf = new StringBuilder();
        buf.append(ar[0]);
        for (int i = 1; i < ar.length; i++) {
            buf.append(delim).append(ar[i]);
        }
        return buf.toString();
    }

    public static String join(List<String> ar, String delim) {
        return join(ar.toArray(new String[ar.size()]), delim);
    }

    public static String[] split(String str, char delimiter, boolean trim) {
        int s = 0;
        int e = str.indexOf(delimiter, s);
        if (e == -1) {
            if (trim) {
                str = str.trim();
            }
            return new String[] { str };
        }
        List<String> ar = new ArrayList<String>();
        do {
            String segment = str.substring(s, e);
            if (trim) {
                segment = segment.trim();
            }
            ar.add(segment);
            s = e + 1;
            e = str.indexOf(delimiter, s);
        } while (e != -1);

        int len = str.length();
        if (s < len) {
            String segment = str.substring(s);
            if (trim) {
                segment = segment.trim();
            }
            ar.add(segment);
        } else {
            ar.add("");
        }

        return ar.toArray(new String[ar.size()]);
    }

}
