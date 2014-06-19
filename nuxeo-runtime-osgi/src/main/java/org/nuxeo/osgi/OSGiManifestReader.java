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

package org.nuxeo.osgi;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class OSGiManifestReader {

    private static final Log log = LogFactory.getLog(OSGiManifestReader.class);

    private static final Pattern PARAMS_PATTERN = Pattern.compile("\\s*([^:\\s]+)\\s*:=\\s*([^;\\s]+)\\s*;?");

    public static final String COMPONENT_HEADER = "Nuxeo-Component";

    public static final String ALLOW_HOST_OVERRIDE = "Nuxeo-AllowOverride";

    public static final String WEB_MODULE = "Nuxeo-WebModule";

    public static String[] CUSTOM_HEADERS = { COMPONENT_HEADER, WEB_MODULE,
            ALLOW_HOST_OVERRIDE };

    static { // we can add dynamically new headers through system properties
        String h = System.getProperty("org.nuxeo.manifest.headers");
        if (h != null) {
            CUSTOM_HEADERS = StringUtils.split(h, ',');
            for (int i = 0; i < CUSTOM_HEADERS.length; ++i) {
                CUSTOM_HEADERS[i] = CUSTOM_HEADERS[i].trim();
            }
        }
    }

    protected final OSGiBundleFile file;

    // Utility class
    protected OSGiManifestReader(OSGiBundleFile file) {
        this.file = file;
    }

    public Dictionary<String, String> getHeadersFromJar(URL url)
            throws IOException {
        JarFile file = new JarFile(new File(url.getFile()));
        try {
            Manifest mf = file.getManifest();
            if (mf != null) {
                try {
                    return getHeaders(mf);
                } catch (BundleException e) {
                    log.error(e, e);
                }
            }
        } finally {
            file.close();
        }
        // not an osgi bundle
        return getDefaultHeaders(url.toExternalForm());
    }

    protected Manifest loadManifest(URL url) throws IOException {
        return loadManifest(new File(url.getFile()));
    }

    protected Manifest loadManifest(File file) throws IOException {
        if (file.isFile()) {
            return loadManifest(new JarFile(file));
        }
        if (file.isDirectory()) {
            File mf = new File(new File(file, "META-INF"), "MANIFEST.MF");
            if (!mf.exists()) {
                return null;
            }
            InputStream in = new FileInputStream(mf);
            try {
                return new Manifest(in);
            } finally {
                in.close();
            }
        }
        return null;
    }

    protected Manifest loadManifest(JarFile file) throws IOException {
        try {
            return file.getManifest();
        } finally {
            file.close();
        }
    }

    public Dictionary<String, String> getHeadersFromFile(File file)
            throws IOException {
        Manifest mf = loadManifest(file);
        if (mf != null) {
            try {
                return getHeaders(mf);
            } catch (BundleException e) {
                log.error(e, e);
            }
        }
        // not an osgi bundle
        return getDefaultHeaders(file.getAbsolutePath());
    }

    public Dictionary<String, String> getDefaultHeaders(String symbolicName) {
        Dictionary<String, String> headers = new Hashtable<String, String>();
        headers.put(Constants.BUNDLE_SYMBOLICNAME, symbolicName);
        headers.put(Constants.BUNDLE_ACTIVATOR,
                OSGiNullActivator.class.getName());
        return headers;
    }

    public Dictionary<String, String> getHeaders(Manifest mf)
            throws BundleException {
        Attributes attrs = mf.getMainAttributes();
        Hashtable<String, String> headers = new Hashtable<String, String>();
        String symbolicName = attrs.getValue(Constants.BUNDLE_SYMBOLICNAME);
        if (symbolicName == null) {
            symbolicName = attrs.getValue("Extension-Name");
        }
        parseSymbolicName(headers, symbolicName);
        String val = attrs.getValue(Constants.BUNDLE_ACTIVATOR);
        if (val != null) {
            headers.put(Constants.BUNDLE_ACTIVATOR, val.trim());
        }
        val = attrs.getValue(Constants.BUNDLE_ACTIVATIONPOLICY);
        if (val != null) {
            headers.put(Constants.BUNDLE_ACTIVATIONPOLICY, val.trim());
        }
        val = attrs.getValue(Constants.BUNDLE_CLASSPATH);
        if (val != null) {
            headers.put(Constants.BUNDLE_CLASSPATH, val.trim());
        }
        val = attrs.getValue(Constants.BUNDLE_NAME);
        if (val != null) {
            headers.put(Constants.BUNDLE_NAME, val);
        }
        val = attrs.getValue(Constants.BUNDLE_VENDOR);
        if (val != null) {
            headers.put(Constants.BUNDLE_VENDOR, val);
        }
        val = attrs.getValue(Constants.BUNDLE_VERSION);
        if (val != null) {
            headers.put(Constants.BUNDLE_VERSION, val);
        }
        val = attrs.getValue(Constants.BUNDLE_DESCRIPTION);
        if (val != null) {
            headers.put(Constants.BUNDLE_DESCRIPTION, val);
        }
        val = attrs.getValue(Constants.BUNDLE_DOCURL);
        if (val != null) {
            headers.put(Constants.BUNDLE_DOCURL, val);
        }
        val = attrs.getValue(Constants.BUNDLE_COPYRIGHT);
        if (val != null) {
            headers.put(Constants.BUNDLE_COPYRIGHT, val);
        }
        val = attrs.getValue(Constants.BUNDLE_LOCALIZATION);
        if (val != null) {
            headers.put(Constants.BUNDLE_LOCALIZATION, val);
        }
        val = attrs.getValue(Constants.REQUIRE_BUNDLE);
        if (val != null) {
            headers.put(Constants.REQUIRE_BUNDLE, val);
        }
        val = attrs.getValue(Constants.FRAGMENT_HOST);
        if (val != null) {
            headers.put(Constants.FRAGMENT_HOST, val);
        }
        // Nuxeo headers
        for (String key : CUSTOM_HEADERS) {
            val = attrs.getValue(key);
            if (val != null) {
                headers.put(key, val);
            }
        }
        return headers;
    }

    private static void parseSymbolicName(Dictionary<String, String> headers,
            String name) {
        int p = name.indexOf(';');
        if (p > 0) {
            headers.put(Constants.BUNDLE_SYMBOLICNAME,
                    name.substring(0, p).trim());
            String tail = name.substring(p + 1);
            Matcher m = PARAMS_PATTERN.matcher(tail);
            while (m.find()) {
                headers.put(m.group(1), m.group(2));
            }
        } else {
            headers.put(Constants.BUNDLE_SYMBOLICNAME, name.trim());
        }
    }

    public static String removePropertiesFromHeaderValue(String value) {
        int p = value.indexOf(';');
        if (p > 0) {
            return value.substring(0, p).trim();
        } else {
            return value;
        }
    }

}
