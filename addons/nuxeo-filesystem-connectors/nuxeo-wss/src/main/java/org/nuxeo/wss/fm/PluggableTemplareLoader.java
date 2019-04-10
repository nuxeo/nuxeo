/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Thierry Delprat
 */
package org.nuxeo.wss.fm;

import java.io.IOException;
import java.io.Reader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.TemplateLoader;

/**
 * {@link TemplateLoader} implementation that deletgates work to several underlaying {@link ClassTemplateLoader}.
 * <p>
 * Using the class allow to have buil-in templates that can be overwritten by the backend implementation.
 *
 * @author Thierry Delprat
 */
public class PluggableTemplareLoader implements TemplateLoader {

    private static final Log log = LogFactory.getLog(PluggableTemplareLoader.class);

    protected ClassTemplateLoader defaultLoader;

    protected ClassTemplateLoader additionnalLoader;

    public PluggableTemplareLoader(ClassTemplateLoader defaultLoader) {
        this.defaultLoader = defaultLoader;
    }

    public void closeTemplateSource(Object templateSource) throws IOException {
        defaultLoader.closeTemplateSource(templateSource);
    }

    public Object findTemplateSource(String name) throws IOException {
        Object template = null;
        if (additionnalLoader != null) {
            try {
                template = additionnalLoader.findTemplateSource(name);
            } catch (IOException e) {
                log.error(e, e);
            }
        }
        if (template == null) {
            template = defaultLoader.findTemplateSource(name);
        }
        return template;
    }

    public long getLastModified(Object templateSource) {
        return defaultLoader.getLastModified(templateSource);
    }

    public Reader getReader(Object templateSource, String encoding) throws IOException {
        return defaultLoader.getReader(templateSource, encoding);
    }

    public TemplateLoader getAdditionnalLoader() {
        return additionnalLoader;
    }

    public void setAdditionnalLoader(ClassTemplateLoader additionnalLoader) {
        this.additionnalLoader = additionnalLoader;
    }

}
