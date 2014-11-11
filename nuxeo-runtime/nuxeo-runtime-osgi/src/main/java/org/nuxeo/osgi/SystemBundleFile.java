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
import java.util.jar.Attributes;
import java.util.jar.Manifest;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class SystemBundleFile extends DirectoryBundleFile {

    public SystemBundleFile(File file) {
        super (file, createManifest());
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
