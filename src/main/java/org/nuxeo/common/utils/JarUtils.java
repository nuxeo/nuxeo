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

package org.nuxeo.common.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public final class JarUtils {

    // Utility class.
    private JarUtils() {
    }

    public static Manifest getManifest(File file) {
        try {
            if (file.isDirectory()) {
                return getDirectoryManifest(file);
            } else {
                return getJarManifest(file);
            }
        } catch (IOException ignored) {
            return null;
        }
    }

    public static Manifest getDirectoryManifest(File file) throws IOException {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(new File(file, "META-INF/MANIFEST.MF"));
            return new Manifest(fis);
        } finally {
            if (fis != null) {
                fis.close();
            }
        }
    }

    public static Manifest getJarManifest(File file) throws IOException {
        JarFile jar = null;
        try {
            jar = new JarFile(file);
            return jar.getManifest();
        } finally {
            if (jar != null) {
                jar.close();
            }
        }
    }

    public static Manifest getManifest(URL url) {
        try {
            return new JarFile(new File(url.getFile())).getManifest();
        } catch (IOException e) {
            return null;
        }
    }

}
