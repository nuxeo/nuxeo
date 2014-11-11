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
import org.nuxeo.ecm.platform.annotations.api.AnnotationException;
import org.nuxeo.ecm.platform.annotations.repository.DefaultNuxeoUriResolver;
import org.nuxeo.ecm.platform.annotations.repository.service.AnnotatedDocumentEventListener;
import org.nuxeo.ecm.platform.annotations.repository.service.AnnotationsRepositoryConfigurationService;
import org.nuxeo.ecm.platform.annotations.service.EventListener;
import org.nuxeo.runtime.api.Framework;

/**
 * @author Alexandre Russel
 *
 */
public class AnnotationEventListener implements EventListener {
    private List<AnnotatedDocumentEventListener> listeners;

    private final DefaultNuxeoUriResolver resolver = new DefaultNuxeoUriResolver();

    public void afterAnnotationCreated(Principal principal, Annotation annotation)
            throws AnnotationException {
        for (AnnotatedDocumentEventListener listener : getListeners()) {
            listener.afterAnnotationCreated((NuxeoPrincipal) principal, getDocumentLocation(annotation),
                    annotation);
        }
    }

    private DocumentLocation getDocumentLocation(Annotation annotation) throws AnnotationException {
        URI annotates = annotation.getAnnotates();
        return resolver.getDocumentLocation(annotates);
    }

    private List<AnnotatedDocumentEventListener> getListeners()
            throws AnnotationException {
        if (listeners == null) {
            synchronized (this) {
                AnnotationsRepositoryConfigurationService service;
                try {
                    service = Framework.getService(AnnotationsRepositoryConfigurationService.class);
                } catch (Exception e) {
                    throw new AnnotationException(e);
                }
                listeners = service.getEventListeners();
            }
        }
        return listeners;
    }

    public void afterAnnotationDeleted(Principal principal, Annotation annotation)
            throws AnnotationException {
        for (AnnotatedDocumentEventListener listener : getListeners()) {
            listener.afterAnnotationDeleted((NuxeoPrincipal) principal, getDocumentLocation(annotation),
                    annotation);
        }
    }

    public void afterAnnotationRead(Principal principal, Annotation annotation)
            throws AnnotationException {
        for (AnnotatedDocumentEventListener listener : getListeners()) {
            listener.afterAnnotationRead((NuxeoPrincipal) principal, getDocumentLocation(annotation), annotation);
        }
    }

    public void afterAnnotationUpdated(Principal principal, Annotation annotation)
            throws AnnotationException {
        for (AnnotatedDocumentEventListener listener : getListeners()) {
            listener.afterAnnotationUpdated((NuxeoPrincipal) principal, getDocumentLocation(annotation),
                    annotation);
        }
    }

    public void beforeAnnotationCreated(Principal principal, Annotation annotation)
            throws AnnotationException {
        for (AnnotatedDocumentEventListener listener : getListeners()) {
            listener.beforeAnnotationCreated((NuxeoPrincipal) principal, getDocumentLocation(annotation),
                    annotation);
        }
    }

    public void beforeAnnotationDeleted(Principal principal, Annotation annotation)
            throws AnnotationException {
        for (AnnotatedDocumentEventListener listener : getListeners()) {
            listener.beforeAnnotationDeleted((NuxeoPrincipal) principal, getDocumentLocation(annotation),
                    annotation);
        }
    }

    public void beforeAnnotationRead(Principal principal, String annotationId) throws AnnotationException {
        for (AnnotatedDocumentEventListener listener : getListeners()) {
            listener.beforeAnnotationRead((NuxeoPrincipal) principal, annotationId);
        }
    }

    public void beforeAnnotationUpdated(Principal principal, Annotation annotation)
            throws AnnotationException {
        for (AnnotatedDocumentEventListener listener : getListeners()) {
            listener.beforeAnnotationUpdated((NuxeoPrincipal) principal, getDocumentLocation(annotation),
                    annotation);
        }
    }

}
