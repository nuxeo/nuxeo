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

package org.nuxeo.ecm.platform.annotations.proxy;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.annotations.api.Annotation;
import org.nuxeo.ecm.platform.annotations.api.AnnotationException;
import org.nuxeo.ecm.platform.annotations.api.AnnotationManager;
import org.nuxeo.ecm.platform.annotations.api.AnnotationsService;
import org.nuxeo.ecm.platform.annotations.api.UriResolver;
import org.nuxeo.ecm.platform.annotations.service.AnnotabilityManager;
import org.nuxeo.ecm.platform.annotations.service.AnnotationConfigurationService;
import org.nuxeo.ecm.platform.annotations.service.AnnotationsServiceImpl;
import org.nuxeo.ecm.platform.annotations.service.EventListener;
import org.nuxeo.ecm.platform.annotations.service.PermissionManager;
import org.nuxeo.ecm.platform.annotations.service.URLPatternFilter;
import org.nuxeo.ecm.platform.relations.api.Graph;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:arussel@nuxeo.com">Alexandre Russel</a>
 *
 */
public class AnnotationServiceProxy implements AnnotationsService {

    private static final Log log = LogFactory.getLog(AnnotationServiceProxy.class);

    private final AnnotationManager annotationManager = new AnnotationManager();

    private AnnotationConfigurationService configurationService;

    private AnnotabilityManager annotabilityManager;

    private AnnotationsServiceImpl service;

    private URLPatternFilter filter;

    private UriResolver resolver;

    private PermissionManager permissionManager;

    private List<EventListener> listeners;

    public void initialise() {
        try {
            service = new AnnotationsServiceImpl();
            configurationService = Framework.getService(AnnotationConfigurationService.class);
        } catch (Exception e) {
            log.error(e);
        }
        filter = configurationService.getUrlPatternFilter();
        resolver = configurationService.getUriResolver();
        annotabilityManager = configurationService.getAnnotabilityManager();
        permissionManager = configurationService.getPermissionManager();
        listeners = configurationService.getListeners();
    }

    public Annotation addAnnotation(Annotation annotation, NuxeoPrincipal user,
            String baseUrl) throws AnnotationException {
        checkUrl(annotation);
        Annotation translatedAnnotation = getTranslatedAnnotation(annotation);
        if (!annotabilityManager.isAnnotable(annotation.getAnnotates())) {
            throw new AnnotationException("Not annotable uri: "
                    + annotation.getAnnotates());
        }
        checkPermission(annotation, user,
                configurationService.getCreateAnnotationPermission());
        for (EventListener listener : listeners) {
            listener.beforeAnnotationCreated(user, translatedAnnotation);
        }
        Annotation tmpResult = service.addAnnotation(translatedAnnotation,
                user, baseUrl);
        for (EventListener listener : listeners) {
            listener.afterAnnotationCreated(user, tmpResult);
        }
        return annotationManager.translateAnnotationFromRepo(resolver, baseUrl,
                tmpResult);
    }

    private void checkPermission(Annotation annotation, NuxeoPrincipal user,
            String permission) throws AnnotationException {
        if (!permissionManager.check(user, permission,
                annotation.getAnnotates())) {
            throw new AnnotationException(user
                    + " allowed to query annotation.");
        }
    }

    private Annotation getTranslatedAnnotation(Annotation annotation)
            throws AnnotationException {
        Annotation translatedAnnotation = annotationManager.translateAnnotationToRepo(
                resolver, annotation);
        return translatedAnnotation;
    }

    private void checkUrl(Annotation annotation) throws AnnotationException {
        try {
            URI uri = annotation.getAnnotates();
            if (uri.toASCIIString().startsWith("urn:")) {
                return;
            }
            String url = uri.toURL().toString();
            if (!filter.allow(url)) {
                throw new AnnotationException("Not allowed to annoates: " + url);
            }
        } catch (MalformedURLException e) {
            throw new AnnotationException(e);
        }
    }

    public void deleteAnnotation(Annotation annotation, NuxeoPrincipal user)
            throws AnnotationException {
        checkPermission(annotation, user,
                configurationService.getDeleteAnnotationPermission());
        Annotation translatedAnnotation = getTranslatedAnnotation(annotation);
        for (EventListener listener : listeners) {
            listener.beforeAnnotationDeleted(user, translatedAnnotation);
        }
        service.deleteAnnotation(translatedAnnotation, user);
        for (EventListener listener : listeners) {
            listener.afterAnnotationDeleted(user, translatedAnnotation);
        }
    }

    public void deleteAnnotationFor(URI uri, Annotation annotation,
            NuxeoPrincipal user) throws AnnotationException {
        checkPermission(annotation, user,
                configurationService.getDeleteAnnotationPermission());
        Annotation translatedAnnotation = getTranslatedAnnotation(annotation);
        for (EventListener listener : listeners) {
            listener.beforeAnnotationDeleted(user, translatedAnnotation);
        }
        service.deleteAnnotationFor(resolver.translateToGraphURI(uri), translatedAnnotation, user);
        for (EventListener listener : listeners) {
            listener.afterAnnotationDeleted(user, translatedAnnotation);
        }
    }

    public Annotation getAnnotation(String annotationId, NuxeoPrincipal user,
            String baseUrl) throws AnnotationException {
        for (EventListener listener : listeners) {
            listener.beforeAnnotationRead(user, annotationId);
        }
        Annotation result = service.getAnnotation(annotationId, user, null);
        checkPermission(result, user,
                configurationService.getReadAnnotationPermission());
        for (EventListener listener : listeners) {
            listener.afterAnnotationRead(user, result);
        }
        return annotationManager.translateAnnotationFromRepo(resolver, baseUrl,
                result);
    }

    public Graph getAnnotationGraph() throws AnnotationException {
        return service.getAnnotationGraph();
    }

    public List<Annotation> queryAnnotations(URI uri,
            Map<String, String> filters, NuxeoPrincipal user)
            throws AnnotationException {
        String baseUrl = null;
        if (!uri.toString().startsWith("urn")) {
            baseUrl = resolver.getBaseUrl(uri);
        }
        List<Annotation> tempResult = service.queryAnnotations(
                resolver.translateToGraphURI(uri), filters, user);
        List<Annotation> result = new ArrayList<Annotation>();
        for (Annotation annotation : tempResult) {
            Annotation translatedAnnotation = annotationManager.translateAnnotationFromRepo(
                    resolver, baseUrl, annotation);
            for (EventListener listener : listeners) {
                listener.afterAnnotationRead(user, translatedAnnotation);
            }
            checkPermission(translatedAnnotation, user,
                    configurationService.getReadAnnotationPermission());
            result.add(translatedAnnotation);
        }
        return result;
    }

    public Annotation updateAnnotation(Annotation annotation,
            NuxeoPrincipal user, String baseUrl) throws AnnotationException {
        checkPermission(annotation, user,
                configurationService.getUpdateAnnotationPermission());
        for (EventListener listener : listeners) {
            listener.beforeAnnotationUpdated(user, annotation);
        }
        Annotation result = service.updateAnnotation(
                getTranslatedAnnotation(annotation), user, baseUrl);
        for (EventListener listener : listeners) {
            listener.afterAnnotationUpdated(user, result);
        }
        return annotationManager.translateAnnotationFromRepo(resolver, baseUrl,
                result);
    }
}
