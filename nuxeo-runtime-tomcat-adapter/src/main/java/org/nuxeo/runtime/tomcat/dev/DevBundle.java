package org.nuxeo.runtime.tomcat.dev;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     slacoin
 */

/**
 * 
 * Bundle descriptor for hot deployment
 * 
 * @since 5.5
 *
 */
public class DevBundle implements Serializable {

    private static final long serialVersionUID = 1L;

    protected String name; // the bundle symbolic name if not a lib

    protected final DevBundleType devBundleType;

    protected final String path;

    public static DevBundle[] parseDevBundleLines(InputStream is)
            throws IOException {
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(is));
        try {
            ArrayList<DevBundle> bundles = new ArrayList<DevBundle>();
            String line = reader.readLine();
            while (line != null) {
                line = line.trim();
                if (line.length() > 0 && !line.startsWith("#")) {
                    bundles.add(parseDevBundleLine(line));
                }
                line = reader.readLine();
            }
            return bundles.toArray(new DevBundle[bundles.size()]);
        } finally {
            reader.close();
        }
    }

    public static DevBundle parseDevBundleLine(String line)
            throws MalformedURLException {
        int index = line.indexOf(':');
        String typename = line.substring(0, index);
        typename = typename.substring(0,1).toUpperCase() + typename.substring(1);
        String path = line.substring(index + 1);
        return new DevBundle(path,
                DevBundleType.valueOf(typename));
    }

    public DevBundle(String path, DevBundleType devBundleType) {
        this.path = path;
        this.devBundleType = devBundleType;
    }

    public URL url() throws IOException {
        return new File(path).toURI().toURL();
    }

    public File file() {
        return new File(path);
    }

    public String getName() {
        return name;
    }

    public String getPath() {
        return file().getAbsolutePath();
    }
}
