/* 
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.rendering.fm;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;

import org.nuxeo.ecm.platform.rendering.api.ResourceLocator;

import freemarker.cache.TemplateLoader;
import freemarker.cache.URLTemplateLoader;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ResourceTemplateLoader implements TemplateLoader {

    protected ResourceLocator locator;

    protected final MyURLTemplateLoader urlLoader;

    protected final MyFileTemplateLoader fileLoader;

    public ResourceTemplateLoader(ResourceLocator locator) {
        this.locator = locator;
        urlLoader = new MyURLTemplateLoader();
        fileLoader = new MyFileTemplateLoader();
    }

    public void setLocator(ResourceLocator locator) {
        this.locator = locator;
    }

    public ResourceLocator getLocator() {
        return locator;
    }

    public void closeTemplateSource(Object templateSource) throws IOException {
        if (templateSource instanceof File) {
            fileLoader.closeTemplateSource(templateSource);
        } else if (templateSource instanceof URL) {
            urlLoader.closeTemplateSource(templateSource);
        }
    }

    public Object findTemplateSource(String name) throws IOException {
        if (name.startsWith("fs://")) { // hack for absolute paths - see
                                        // FreemarkerEngine#render()
            name = name.substring(5);
        } else if (name.contains(":/")) {
            return urlLoader.findTemplateSource(name);
        }
        Object obj = fileLoader.findTemplateSource(name);
        if (obj != null) {
            return obj;
        }
        return urlLoader.findTemplateSource(name);
    }

    public long getLastModified(Object templateSource) {
        if (templateSource instanceof File) {
            return fileLoader.getLastModified(templateSource);
        } else {
            return urlLoader.getLastModified(templateSource);
        }
    }

    public Reader getReader(Object templateSource, String encoding)
            throws IOException {
        if (templateSource instanceof File) {
            return fileLoader.getReader(templateSource, encoding);
        } else {
            return urlLoader.getReader(templateSource, encoding);
        }
    }

    class MyURLTemplateLoader extends URLTemplateLoader {
        @Override
        protected URL getURL(String arg0) {
            if (locator != null) {
                return locator.getResourceURL(arg0);
            }
            try {
                return new URL(arg0);
            } catch (MalformedURLException e) {
                return null;
            }
        }
    }

    class MyFileTemplateLoader implements TemplateLoader {
        public void closeTemplateSource(Object templateSource)
                throws IOException {
            // do nothing
        }

        public Object findTemplateSource(String name) throws IOException {
            if (locator != null) {
                File file = locator.getResourceFile(name);
                if (file != null) {
                    return file.getCanonicalFile();
                }
            }
            File file = new File(name).getCanonicalFile();
            if (file.isFile()) {
                return file;
            }
            return null;
        }

        public long getLastModified(Object templateSource) {
            try {
                return ((File) templateSource).lastModified();
            } catch (ClassCastException e) {
                throw new IllegalArgumentException("templateSource is a: "
                        + templateSource.getClass().getName());
            }
        }

        public Reader getReader(Object templateSource, String encoding)
                throws IOException {
            try {
                return new InputStreamReader(new FileInputStream(
                        (File) templateSource), encoding);
            } catch (ClassCastException e) {
                throw new IllegalArgumentException("templateSource is a: "
                        + templateSource.getClass().getName());
            }
        }

    }

}
