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
 * $Id$
 */

package org.nuxeo.common.xmap;

import java.util.ArrayList;
import java.util.List;

/**
 * @author  <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class Path {

    public static final String[] EMPTY_SEGMENTS = new String[0];

    final String path;
    String[] segments;
    String attribute;

    public Path(String path) {
        this.path = path;
        parse(path);
    }


    @Override
    public String toString() {
        return path;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof Path) {
            return ((Path) obj).path.equals(path);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return path.hashCode();
    }

    private void parse(String path) {
        List<String> seg = new ArrayList<String>();
        StringBuilder buf = new StringBuilder();
        char[] chars = path.toCharArray();
        boolean attr = false;
        for (char c : chars) {
            switch (c) {
                case'/':
                    seg.add(buf.toString());
                    buf.setLength(0);
                    break;
                case'@':
                    attr = true;
                    seg.add(buf.toString());
                    buf.setLength(0);
                    break;
                default:
                    buf.append(c);
                    break;
            }
        }
        if (buf.length() > 0) {
            if (attr) {
                attribute = buf.toString();
            } else {
                seg.add(buf.toString());
            }
        }
        int size = seg.size();
        if (size == 1 && seg.get(0).length() == 0) {
            segments = EMPTY_SEGMENTS;
        } else {
            segments = seg.toArray(new String[size]);
        }
    }

}
