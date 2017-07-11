/*
 * (C) Copyright 2006-2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     slacoin
 *     Kevin Leturc <kleturc@nuxeo.com>
 */
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

/**
 * Bundle descriptor for hot deployment
 *
 * @since 5.5
 */
public class DevBundle implements Serializable {

    private static final long serialVersionUID = 1L;

    protected String name; // the bundle symbolic name if not a lib

    protected final DevBundleType devBundleType;

    protected final String path;

    public static DevBundle[] parseDevBundleLines(InputStream is) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
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

    public static DevBundle parseDevBundleLine(String line) throws MalformedURLException {
        int index = line.indexOf(':');
        String typename = line.substring(0, index);
        typename = typename.substring(0, 1).toUpperCase() + typename.substring(1);
        String path = line.substring(index + 1);
        return new DevBundle(path, DevBundleType.valueOf(typename));
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
