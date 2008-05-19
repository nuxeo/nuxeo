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


    //TODO should remove the typed file name
    public ScriptFile(File file) throws IOException {
        String name = file.getName();
        int p = name.lastIndexOf('.');
        if (p > -1) {
            ext = name.substring(p + 1);
        } else { // by default use ftl
            ext = "ftl";
        }
        this.file = file.getCanonicalFile();
    }

    public File getFile() {
        return file;
    }

    public String getExtension() {
        return ext;
    }

    @Override
    public String toString() {
        return file.toString();
    }

}
