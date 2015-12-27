/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and others.
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

    Graph getAnnotationGraph();

    /**
     * Finds Annotation ids according to URI.
     */
    List<Annotation> queryAnnotations(URI uri, NuxeoPrincipal user);

    /**
     * Finds number of Annotation according to URI.
     */
    int getAnnotationsCount(URI uri, NuxeoPrincipal user);

    /**
     * Returns an Annotation according to its Id.
     */
    Annotation getAnnotation(String annotationId, NuxeoPrincipal user, String baseUrl);

    /**
     * Adds an annotation to the target URL.
     *
     * @return the Annotation
     */
    Annotation addAnnotation(Annotation annotation, NuxeoPrincipal user, String baseUrl);

    /**
     * Updates an annotation.
     */
    Annotation updateAnnotation(Annotation annotation, NuxeoPrincipal user, String baseUrl);

    /**
     * Deletes an annotation.
     */
    void deleteAnnotation(Annotation annotation, NuxeoPrincipal user);

    void deleteAnnotationFor(URI uri, Annotation annotation, NuxeoPrincipal user);

}
