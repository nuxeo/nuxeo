/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.annotations.ejb;

import java.net.URI;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.ejb.Stateless;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.annotations.api.Annotation;
import org.nuxeo.ecm.platform.annotations.api.AnnotationException;
import org.nuxeo.ecm.platform.annotations.api.AnnotationsService;
import org.nuxeo.ecm.platform.relations.api.Graph;
import org.nuxeo.runtime.api.Framework;

@Stateless
public class AnnotationServiceBean implements AnnotationsService {

    private static final Log log = LogFactory.getLog(AnnotationServiceBean.class);

    private AnnotationsService service;

    @PostConstruct
    public void initialise() {
        try {
            service = Framework.getLocalService(AnnotationsService.class);
        } catch (Exception e) {
            log.error(e);
        }
    }

    public Annotation addAnnotation(Annotation annotation, NuxeoPrincipal user,
            String baseUrl) throws AnnotationException {
        return service.addAnnotation(annotation, user, baseUrl);
    }

    public void deleteAnnotation(Annotation annotation, NuxeoPrincipal user)
            throws AnnotationException {
        service.deleteAnnotation(annotation, user);
    }

    public void deleteAnnotationFor(URI uri, Annotation annotation,
            NuxeoPrincipal user) throws AnnotationException {
        service.deleteAnnotationFor(uri, annotation, user);
    }

    public Annotation getAnnotation(String annotationId, NuxeoPrincipal user,
            String baseUrl) throws AnnotationException {
        return service.getAnnotation(annotationId, user, baseUrl);
    }

    public List<Annotation> queryAnnotations(URI uri,
            Map<String, String> filters, NuxeoPrincipal user)
            throws AnnotationException {
        return service.queryAnnotations(uri, filters, user);
    }

    public Annotation updateAnnotation(Annotation annotation,
            NuxeoPrincipal user, String baseUrl) throws AnnotationException {
        return service.updateAnnotation(annotation, user, baseUrl);
    }

    public Graph getAnnotationGraph() throws AnnotationException {
        return service.getAnnotationGraph();
    }

}
