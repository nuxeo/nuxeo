/*******************************************************************************
 * Copyright (c) 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.nuxeo.ecm.platform.ui.web.application.config;

import java.lang.annotation.Annotation;
import java.net.URI;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletContext;

import org.apache.commons.logging.LogFactory;

import com.sun.faces.spi.AnnotationProvider;

public class JSFAnnotationProvider extends AnnotationProvider {

    protected final AnnotationProvider base;

    public JSFAnnotationProvider(ServletContext sc, AnnotationProvider aProvider) {
        super(sc);
        base = aProvider;
    }

    @Override
    public Map<Class<? extends Annotation>, Set<Class<?>>> getAnnotatedClasses(
            Set<URI> urls) {
        if (JSFContainerInitializer.self == null) {
            LogFactory.getLog(JSFAnnotationProvider.class).warn("container scanned classes unavailable, applying default scanning");
            return base.getAnnotatedClasses(urls);
        }
        return JSFContainerInitializer.self.index;
    }

}
