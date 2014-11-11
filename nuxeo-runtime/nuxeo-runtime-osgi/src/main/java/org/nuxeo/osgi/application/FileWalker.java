/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.osgi.application;

import java.io.File;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class FileWalker {

    public static final int CONTINUE = 0;
    public static final int RETURN = 1;
    public static final int BREAK = 2;

    public static int walk(File root, Visitor visitor) {
        int ret = 0; // -1 stop walking, 0 continue, 1 break walking
        for (File file : root.listFiles()) {
            if (file.isDirectory()) {
                ret = visitor.visitDirectory(file);
                if (ret == RETURN) {
                    return RETURN;
                } else if (ret == BREAK) {
                    return CONTINUE;
                } else { // go down in the tree
                    ret = walk(file, visitor);
                    if (ret == RETURN) {
                        return RETURN;
                    }
                }
            } else {
                ret = visitor.visitFile(file);
                if (ret == RETURN) {
                    return RETURN;
                } else if (ret == BREAK) {
                    return CONTINUE;
                }
            }
        }
        return CONTINUE;
    }

    public abstract static class Visitor {
        public int visitFile(File file) {
            // do something here
            return CONTINUE;
        }
        public int visitDirectory(File file) {
            // do something here
            return CONTINUE;
        }
    }

}
