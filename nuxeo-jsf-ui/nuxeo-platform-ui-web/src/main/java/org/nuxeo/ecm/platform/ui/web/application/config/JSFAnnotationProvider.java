/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ecm.platform.ui.web.application.config;

import java.lang.annotation.Annotation;
import java.net.URI;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.sun.faces.spi.AnnotationProvider;

/**
 * TODO.
 *
 * @since 6.0
 */
public class JSFAnnotationProvider extends AnnotationProvider {

    private static final Log log = LogFactory.getLog(JSFAnnotationProvider.class);

    protected final AnnotationProvider base;

    public JSFAnnotationProvider(ServletContext sc, AnnotationProvider aProvider) {
        super(sc);
        base = aProvider;
    }

    @Override
    public Map<Class<? extends Annotation>, Set<Class<?>>> getAnnotatedClasses(Set<URI> urls) {
        if (JSFContainerInitializer.self == null) {
            log.info("Container scanned classes unavailable, applying default scanning");
            return base.getAnnotatedClasses(urls);
        }
        return JSFContainerInitializer.self.index;
    }

}
