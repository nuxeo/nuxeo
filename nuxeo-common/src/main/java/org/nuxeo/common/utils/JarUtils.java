/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
