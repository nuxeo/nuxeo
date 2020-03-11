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

package org.nuxeo.osgi;

import java.io.File;
import java.io.IOException;
import java.util.jar.JarFile;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class NestedJarBundleFile extends JarBundleFile {

    protected final String location;

    @SuppressWarnings("resource")
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
