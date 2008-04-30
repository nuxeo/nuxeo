/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.webengine.scripting;

import java.io.File;
import java.io.IOException;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public final class ScriptFile {

    protected File file;
    protected String ext;
    protected String path;


    //TODO should remove the typed file name
    public ScriptFile(File root, String path, String type) throws IOException {
        int p = path.lastIndexOf('.');
        String typedPath = null;
        if (p > -1) {
            ext = path.substring(p + 1);
            if (type != null) {
                typedPath = path.substring(0, p) + '_' + type + '.' + ext;
            }
        } else { // by default use ftl
            ext = "ftl";
            path+=".ftl";
            if (type != null) {
                typedPath = path + '_' + type;
            }
        }
        if (typedPath != null) {
            file = new File(root, typedPath);
            this.path = typedPath;
            if (!file.isFile()) {
                file = new File(root, path);
                this.path = path;
            }
        } else {
            file = new File(root, path);
            this.path = path;
        }
        file = file.getCanonicalFile(); // resolve sym links
    }

    public String getPath() {
        return path;
    }

    public File getFile() {
        return file;
    }

    public String getExtension() {
        return ext;
    }

}
