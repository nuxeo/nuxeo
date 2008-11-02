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

package org.nuxeo.osgi;

import java.io.File;
import java.io.IOException;
import java.util.jar.JarFile;


/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class NestedJarBundleFile extends JarBundleFile {

    protected final String location;

    public NestedJarBundleFile(String location, File file) throws IOException {
        this(location, new JarFile(file));
    }

    public NestedJarBundleFile(String location, JarFile jarFile) {
        super(jarFile);
        this.location = location;
    }

    @Override
    public String getFileName() {
        int p = location.lastIndexOf('/');
        if (p == -1) {
            return location;
        }
        if (p == 0) {
            return "";
        }
        return location.substring(p + 1);
    }

    @Override
    public String getLocation() {
        return location;
    }

}
