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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.webengine.client.console;

import java.util.List;

import jline.Completor;

import org.nuxeo.ecm.client.Path;

/**
 * Auto-completes remote item names.
 * 
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ItemNameCompletor implements Completor {

    private final JLineConsole console;

    public ItemNameCompletor(JLineConsole console) {
        this.console = console;
    }

    public int complete(String buf, int off, List candidates) {
        if (buf == null) {
            buf = "";
        }
        Path path = new Path(buf);
        String prefix = path.lastSegment();
        if (path.hasTrailingSeparator()) {
            prefix = "";
        } else {
            path = path.removeLastSegments(1);
        }

        if (prefix == null) {
            prefix = "";
        }

        try {
            List<String> names = console.getClient().ls();
            if (names == null || names.isEmpty()) {
                return -1;
            }

            if (buf.length() == 0) {
                for (String name : names) {
                    candidates.add(name);
                }
            } else {
                for (String name : names) {
                    if (name.startsWith(prefix)) {
                        candidates.add(name);
                    }
                }
            }

            return buf.length()-prefix.length();

        } catch (Exception e) {
            return -1;
        }
    }

}
