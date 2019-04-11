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
import java.util.jar.Attributes;
import java.util.jar.Manifest;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class SystemBundleFile extends DirectoryBundleFile {

    public SystemBundleFile(File file) throws IOException {
        super(file, createManifest());
    }

    public static Manifest createManifest() {
        Manifest mf = new Manifest();
        Attributes attrs = mf.getMainAttributes();
        attrs.putValue("Bundle-SymbolicName", "org.nuxeo.osgi.app");
        attrs.putValue("Bundle-Name", "Nuxeo App System Bundle");
        attrs.putValue("Bundle-Vendor", "Nuxeo");
        attrs.putValue("Bundle-Version", "1.0.0");
        return mf;
    }

}
