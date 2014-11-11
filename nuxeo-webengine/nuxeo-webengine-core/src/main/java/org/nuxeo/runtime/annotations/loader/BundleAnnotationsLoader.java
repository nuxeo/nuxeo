/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *
 * $Id$
 */

package org.nuxeo.runtime.annotations.loader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;

/**
 * @deprecated not used
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class BundleAnnotationsLoader implements BundleListener {

    private static final Log log = LogFactory.getLog(BundleAnnotationsLoader.class);

    protected static final BundleAnnotationsLoader instance = new BundleAnnotationsLoader();

    public static BundleAnnotationsLoader getInstance() {
        return instance;
    }

    protected final Map<String, AnnotationLoader> loaders;

    protected final Map<String, List<Entry>> pendings;

    public BundleAnnotationsLoader() {
        loaders = new HashMap<String, AnnotationLoader>();
        pendings = new HashMap<String, List<Entry>>();
    }

    public synchronized void addLoader(String annotationType,
            AnnotationLoader loader) {
        loaders.put(annotationType, loader);
        List<Entry> entries = pendings.remove(annotationType);
        if (entries != null) {
            for (Entry entry : entries) {
                try {
                    loader.loadAnnotation(entry.bundle, annotationType,
                            entry.className, entry.args);
                } catch (Exception e) {
                    log.error("Failed to load annotation: " + annotationType
                            + "@" + entry.className, e);
                }
            }
        }
    }

    public void loadAnnotationsFromDeployedBundles(Bundle bundle)
            throws IOException {
        Bundle[] bundles = bundle.getBundleContext().getBundles();
        for (Bundle b : bundles) {
            loadAnnotations(b);
        }
    }

    public void loadAnnotations(Bundle bundle) throws IOException {
        URL url = bundle.getEntry("OSGI-INF/annotations");
        if (url != null) {
            InputStream in = url.openStream();
            try {
                log.info("Loading annotations from bundle: "
                        + bundle.getSymbolicName());
                for (String line : readLines(in)) {
                    loadAnnotation(bundle, line);
                }
            } finally {
                in.close();
            }
        }
    }

    protected void loadAnnotation(Bundle bundle, String line) {
        String[] ar = parse(line);
        if (ar.length < 2) {
            log.error("Invalid annotation entry key '" + line + "' in bundle '"
                    + bundle.getLocation() + "'.");
            return;
        }
        String className = ar[0];
        String annoType = ar[1];
        if (ar.length > 2) {
            String[] tmp = new String[ar.length - 2];
            System.arraycopy(ar, 2, tmp, 0, tmp.length);
            ar = tmp;
        }
        loadAnnotation(bundle, annoType, className, ar);
    }

    public static String[] parse(String str) {
        ArrayList<String> list = new ArrayList<String>();
        char[] chars = str.toCharArray();
        boolean esc = false;
        StringBuilder buf = new StringBuilder();
        char c = 0;
        for (int i = 0; i < chars.length; i++) {
            c = chars[i];
            switch (c) {
            case '\\':
                if (!esc) {
                    esc = true;
                } else {
                    esc = false;
                    buf.append(c);
                }
                break;
            case '|':
                if (!esc) {
                    list.add(buf.toString());
                    buf.setLength(0);
                } else {
                    // System.out.println("escaped | >>> "+buf.toString());
                    buf.append(c);
                    esc = false;
                }
                break;
            default:
                buf.append(c);
            }
        }
        if (buf.length() > 0) {
            list.add(buf.toString());
        } else if (c == '|') {
            list.add("");
        }
        return list.toArray(new String[list.size()]);
    }

    protected synchronized void loadAnnotation(Bundle bundle,
            String annotationType, String className, String[] args) {
        AnnotationLoader loader = loaders.get(annotationType);
        if (loader != null) {
            try {
                loader.loadAnnotation(bundle, annotationType, className, args);
            } catch (Exception e) {
                log.error("Failed to load annotation: " + annotationType + "@"
                        + className, e);
            }
        } else { // queue the entry until a loader is registered
            List<Entry> entries = pendings.get(annotationType);
            if (entries == null) {
                entries = new ArrayList<Entry>();
                pendings.put(annotationType, entries);
            }
            entries.add(new Entry(bundle, className, args));
        }
    }

    static class Entry {
        final Bundle bundle;

        final String className;

        final String[] args;

        Entry(Bundle bundle, String className, String[] args) {
            this.bundle = bundle;
            this.className = className;
            this.args = args;
        }
    }

    public void bundleChanged(BundleEvent event) {
        try {
            switch (event.getType()) {
            case BundleEvent.RESOLVED:
                loadAnnotations(event.getBundle());
                break;
            case BundleEvent.UNRESOLVED:
                // TODO implement unload
                // unloadAnnotations(event.getBundle());
                break;
            }
        } catch (IOException e) {
            log.error(e, e);
        }
    }

    public static List<String> readLines(InputStream in) throws IOException {
        List<String> lines = new ArrayList<String>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        String line;
        while ((line = reader.readLine()) != null) {
            lines.add(line);
        }
        return lines;
    }

}
