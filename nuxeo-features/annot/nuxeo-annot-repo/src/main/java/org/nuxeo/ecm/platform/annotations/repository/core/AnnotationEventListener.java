/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Alexandre Russel
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.annotations.repository.core;

import java.net.URI;
import java.security.Principal;
import java.util.List;

import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.annotations.api.Annotation;
import org.nuxeo.ecm.platform.annotations.repository.DefaultNuxeoUriResolver;
import org.nuxeo.ecm.platform.annotations.repository.service.AnnotatedDocumentEventListener;
import org.nuxeo.ecm.platform.annotations.repository.service.AnnotationsRepositoryConfigurationService;
import org.nuxeo.ecm.platform.annotations.service.EventListener;
import org.nuxeo.runtime.api.Framework;

/**
 * @author Alexandre Russel
 */
public class AnnotationEventListener implements EventListener {
    private List<AnnotatedDocumentEventListener> listeners;

    private final DefaultNuxeoUriResolver resolver = new DefaultNuxeoUriResolver();

    public void afterAnnotationCreated(Principal principal, Annotation annotation) {
        for (AnnotatedDocumentEventListener listener : getListeners()) {
            listener.afterAnnotationCreated((NuxeoPrincipal) principal, getDocumentLocation(annotation), annotation);
        }
    }

    private DocumentLocation getDocumentLocation(Annotation annotation) {
        URI annotates = annotation.getAnnotates();
        return resolver.getDocumentLocation(annotates);
    }

    private List<AnnotatedDocumentEventListener> getListeners() {
        if (listeners == null) {
            AnnotationsRepositoryConfigurationService service = Framework.getService(AnnotationsRepositoryConfigurationService.class);
            listeners = service.getEventListeners();
        }
        return listeners;
    }

    public void afterAnnotationDeleted(Principal principal, Annotation annotation) {
        for (AnnotatedDocumentEventListener listener : getListeners()) {
            listener.afterAnnotationDeleted((NuxeoPrincipal) principal, getDocumentLocation(annotation), annotation);
        }
    }

    public void afterAnnotationRead(Principal principal, Annotation annotation) {
        for (AnnotatedDocumentEventListener listener : getListeners()) {
            listener.afterAnnotationRead((NuxeoPrincipal) principal, getDocumentLocation(annotation), annotation);
        }
    }

    public void afterAnnotationUpdated(Principal principal, Annotation annotation) {
        for (AnnotatedDocumentEventListener listener : getListeners()) {
            listener.afterAnnotationUpdated((NuxeoPrincipal) principal, getDocumentLocation(annotation), annotation);
        }
    }

    public void beforeAnnotationCreated(Principal principal, Annotation annotation) {
        for (AnnotatedDocumentEventListener listener : getListeners()) {
            listener.beforeAnnotationCreated((NuxeoPrincipal) principal, getDocumentLocation(annotation), annotation);
        }
    }

    public void beforeAnnotationDeleted(Principal principal, Annotation annotation) {
        for (AnnotatedDocumentEventListener listener : getListeners()) {
            listener.beforeAnnotationDeleted((NuxeoPrincipal) principal, getDocumentLocation(annotation), annotation);
        }
    }

    public void beforeAnnotationRead(Principal principal, String annotationId) {
        for (AnnotatedDocumentEventListener listener : getListeners()) {
            listener.beforeAnnotationRead((NuxeoPrincipal) principal, annotationId);
        }
    }

    public void beforeAnnotationUpdated(Principal principal, Annotation annotation) {
        for (AnnotatedDocumentEventListener listener : getListeners()) {
            listener.beforeAnnotationUpdated((NuxeoPrincipal) principal, getDocumentLocation(annotation), annotation);
        }
    }

}
