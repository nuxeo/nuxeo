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

package org.nuxeo.ecm.platform.annotations.api;

import java.net.URI;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.relations.api.Graph;

/**
 * Interface for the Annotation Service
 *
 * @author tiry
 */
public interface AnnotationsService {

    Graph getAnnotationGraph() throws AnnotationException;

    /**
     * Finds Annotation ids according to URI and filter.
     */
    List<Annotation> queryAnnotations(URI uri, Map<String, String> filters,
            NuxeoPrincipal user) throws AnnotationException;

    /**
     * Returns an Annotation according to its Id.
     */
    Annotation getAnnotation(String annotationId, NuxeoPrincipal user,
            String baseUrl) throws AnnotationException;

    /**
     * Adds an annotation to the target URL.
     *
     * @return the Annotation
     */
    Annotation addAnnotation(Annotation annotation, NuxeoPrincipal user,
            String baseUrl) throws AnnotationException;

    /**
     * Updates an annotation.
     */
    Annotation updateAnnotation(Annotation annotation, NuxeoPrincipal user,
            String baseUrl) throws AnnotationException;

    /**
     * Deletes an annotation.
     */
    void deleteAnnotation(Annotation annotation, NuxeoPrincipal user)
            throws AnnotationException;

    void deleteAnnotationFor(URI uri, Annotation annotation, NuxeoPrincipal user)
            throws AnnotationException;

}
