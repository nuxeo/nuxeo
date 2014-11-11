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

package org.nuxeo.osgi;

import java.io.File;
import java.net.URL;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.JarUtils;
import org.nuxeo.common.utils.StringUtils;
import org.osgi.framework.Constants;

/**
 * @author  <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public final class BundleManifestReader {

    private static final Log log = LogFactory.getLog(BundleManifestReader.class);

    private static final Pattern PARAMS_PATTERN
            = Pattern.compile("\\s*([^:\\s]+)\\s*:=\\s*([^;\\s]+)\\s*;?");

    public static String[] CUSTOM_HEADERS = {"Nuxeo-Component", "Nuxeo-WebModule"};

    static { // we can add dynamically new headers through system properties
        String h = System.getProperty("org.nuxeo.manifest.headers");
        if (h != null) {
            CUSTOM_HEADERS = StringUtils.split(h, ',', true);
        }
    }

    // Utility class
    private BundleManifestReader() {
    }

    public static Dictionary<String, String> getHeadersFromJar(URL url) {
        Manifest mf = JarUtils.getManifest(url);
        if (mf != null) {
            try {
                return getHeaders(mf);
            } catch (Exception e) {
                log.error(e, e);
            }
        }
        // not an osgi bundle
        return getDefaultHeaders(url.toExternalForm());
    }

    public static Dictionary<String, String> getHeadersFromFile(File file) {
        Manifest mf = JarUtils.getManifest(file);
        if (mf != null) {
            try {
                return getHeaders(mf);
            } catch (Exception e) {
                log.error(e, e);
            }
        }
        // not an osgi bundle
        return getDefaultHeaders(file.getAbsolutePath());
    }

    public static Dictionary<String, String> getDefaultHeaders(String symbolicName) {
        Dictionary<String, String> headers = new Hashtable<String, String>();
        headers.put(Constants.BUNDLE_SYMBOLICNAME, symbolicName);
        headers.put(Constants.BUNDLE_ACTIVATOR, NullActivator.class.getName());
        return headers;
    }


    public static Dictionary<String, String> getHeaders(Manifest mf) {
        Attributes attrs = mf.getMainAttributes();
        String symbolicName = attrs.getValue(Constants.BUNDLE_SYMBOLICNAME);
        if (symbolicName == null) {
            return null;
        }
        Hashtable<String, String> headers = new Hashtable<String, String>();
        parseSymbolicName(headers, symbolicName);
        String val = attrs.getValue(Constants.BUNDLE_ACTIVATOR);
        if (val != null) {
            headers.put(Constants.BUNDLE_ACTIVATOR, val.trim());
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
        // Nuxeo headers
        for (String key : CUSTOM_HEADERS) {
            val = attrs.getValue(key);
            if (val != null) {
                headers.put(key, val);
            }
        }
        return headers;
    }

    private static void parseSymbolicName(Dictionary<String, String> headers, String name) {
        int p = name.indexOf(';');
        if (p > 0) {
            headers.put(Constants.BUNDLE_SYMBOLICNAME, name.substring(0, p).trim());
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
