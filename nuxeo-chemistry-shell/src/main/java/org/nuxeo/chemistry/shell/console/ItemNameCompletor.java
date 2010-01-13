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

package org.nuxeo.chemistry.shell.console;

import java.util.List;

import jline.Completor;

import org.nuxeo.chemistry.shell.Path;

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

    public int complete(String buffer, int cursor, List candidates) {
        if (buffer == null) {
            buffer = "";
        }
        Path path = new Path(buffer);
        String prefix = path.getLastSegment();
        if (path.hasTrailingSeparator()) {
            prefix = "";
        } else {
            path = path.removeLastSegments(1);
        }

        if (prefix == null) {
            prefix = "";
        }

        try {
            String[] names = console.getApplication().getContext().entries();
            if (names == null || names.length == 0) {
                return -1;
            }

            if (buffer.length() == 0) {
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

            return buffer.length()-prefix.length();

        } catch (Exception e) {
            return -1;
        }
    }

}
