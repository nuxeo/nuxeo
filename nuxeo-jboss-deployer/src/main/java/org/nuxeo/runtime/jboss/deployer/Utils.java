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
 *     bstefanescu
 */
package org.nuxeo.runtime.jboss.deployer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.util.file.Files;
import org.jboss.util.file.JarUtils;
import org.jboss.virtual.VFSUtils;
import org.jboss.virtual.VirtualFile;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class Utils {

    private static final Log log = LogFactory.getLog(Utils.class);

    private Utils() {
    }

    public static File tryGetFile(URL url) throws Exception {
        String protocol = url.getProtocol().toLowerCase();
        if ("file".equals(protocol)) {
            return getFile(url);
        } else if ("jar".equals(protocol)) {
            return getJarFile(url);
        } else {
            throw new IOException("URL protocol not known: " + protocol);
        }
    }

    public static File getFile(URL url) throws Exception {
        try {
            return new File(url.toURI());
        } catch (URISyntaxException e) {
            return new File(url.getPath());
        }
    }

    public static File getJarFile(URL url) throws Exception {
        String path = url.getPath();
        int i = path.indexOf('!');
        if (i > -1) {
            path = path.substring(0, i);
        }
        return getFile(new URL(path));
    }

    /**
     * Try to convert a virtual file to a real java file. An IOException is
     * thrown if the virtual file cannot be converted to a java file. Virtual
     * files that point to a zip entry will return the zip file containing the
     * entry.
     */
    public static File getFile(VirtualFile vf) throws Exception {
        return tryGetFile(VFSUtils.getRealURL(vf));
    }

    public static File getRealHomeDir(VirtualFile vf) throws Exception {
        return getRealHomeDir(vf, 0);
    }

    /**
     * Unzip only if the given lastModified is greater than the already unzipped
     * home (if any). Must use a value of 0 if the lastModified time is not
     * known.
     */
    public static File getRealHomeDir(VirtualFile vf, long lastModified)
            throws Exception {
        File file = Utils.getFile(vf);
        if (file.isFile()) { // may be an archive
            return getZippedHomeDir(file, vf.getName(), lastModified);
        } else {
            return file;
        }
    }

    protected static File getZippedHomeDir(File jar, String name,
            long lastModified) throws Exception {
        String tmpPath = System.getProperty("jboss.server.temp.dir");
        File tmp = new File(tmpPath);
        File homeDir = new File(tmp, "nuxeo/" + name);
        if (homeDir.isDirectory()) {
            if (lastModified > 0 && lastModified < homeDir.lastModified()) {
                return homeDir;
            } else {
                // remove the existing unzipped nuxeo - TODO not working
                // correctly
                Files.delete(homeDir);
            }
        }
        homeDir.mkdirs();
        InputStream in = new FileInputStream(jar);
        try {
            double s = System.currentTimeMillis();
            JarUtils.unjar(in, homeDir);
            log.info("Unpacking Nuxeo Application took: "
                    + ((System.currentTimeMillis() - s) / 1000) + " sec.");
        } finally {
            in.close();
        }
        return homeDir;
    }

    public static String[] split(String str, char delimiter, boolean trim) {
        int s = 0;
        int e = str.indexOf(delimiter, s);
        if (e == -1) {
            if (trim) {
                str = str.trim();
            }
            return new String[] { str };
        }
        List<String> ar = new ArrayList<String>();
        do {
            String segment = str.substring(s, e);
            if (trim) {
                segment = segment.trim();
            }
            ar.add(segment);
            s = e + 1;
            e = str.indexOf(delimiter, s);
        } while (e != -1);

        int len = str.length();
        if (s < len) {
            String segment = str.substring(s);
            if (trim) {
                segment = segment.trim();
            }
            ar.add(segment);
        } else {
            ar.add("");
        }

        return ar.toArray(new String[ar.size()]);
    }

    /**
     * Expands any variable found in the given expression with the values in the
     * given map.
     * <p>
     * The variable format is ${property_key}.
     *
     * @param expression the expression to expand
     * @param properties a map containing variables
     */
    public static String expandVars(String expression, Map<?, ?> properties) {
        int p = expression.indexOf("${");
        if (p == -1) {
            return expression; // do not expand if not needed
        }

        char[] buf = expression.toCharArray();
        StringBuilder result = new StringBuilder(buf.length);
        if (p > 0) {
            result.append(expression.substring(0, p));
        }
        StringBuilder varBuf = new StringBuilder();
        boolean dollar = false;
        boolean var = false;
        for (int i = p; i < buf.length; i++) {
            char c = buf[i];
            switch (c) {
            case '$':
                dollar = true;
                break;
            case '{':
                if (dollar) {
                    dollar = false;
                    var = true;
                } else {
                    result.append(c);
                }
                break;
            case '}':
                if (var) {
                    var = false;
                    String varName = varBuf.toString();
                    varBuf.setLength(0);
                    // get the variable value
                    Object varValue = properties.get(varName);
                    if (varValue != null) {
                        result.append(varValue.toString());
                    } else { // let the variable as is
                        result.append("${").append(varName).append('}');
                    }
                } else {
                    result.append(c);
                }
                break;
            default:
                if (var) {
                    varBuf.append(c);
                } else {
                    result.append(c);
                }
                break;
            }
        }
        return result.toString();
    }

}
