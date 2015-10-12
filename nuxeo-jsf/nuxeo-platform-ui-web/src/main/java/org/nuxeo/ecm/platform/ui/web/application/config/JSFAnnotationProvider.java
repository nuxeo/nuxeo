/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 */
package org.nuxeo.ecm.platform.ui.web.application.config;

import java.lang.annotation.Annotation;
import java.net.URI;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletContext;

import org.apache.commons.logging.LogFactory;

import com.sun.faces.spi.AnnotationProvider;

/**
 * TODO.
 *
 * @since TODO
 */
public class JSFAnnotationProvider extends AnnotationProvider {

    protected final AnnotationProvider base;

    public JSFAnnotationProvider(ServletContext sc, AnnotationProvider aProvider) {
        super(sc);
        base = aProvider;
    }

    @Override
    public Map<Class<? extends Annotation>, Set<Class<?>>> getAnnotatedClasses(Set<URI> urls) {
        if (JSFContainerInitializer.self == null) {
            LogFactory.getLog(JSFAnnotationProvider.class).warn(
                    "container scanned classes unavailable, applying default scanning");
            return base.getAnnotatedClasses(urls);
        }
        return JSFContainerInitializer.self.index;
    }

}
