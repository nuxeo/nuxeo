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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.osgi.application;

import java.io.File;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
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
