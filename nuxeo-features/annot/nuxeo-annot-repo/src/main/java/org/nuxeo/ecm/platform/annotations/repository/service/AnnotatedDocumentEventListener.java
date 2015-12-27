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

package org.nuxeo.ecm.platform.annotations.repository.service;

import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.annotations.api.Annotation;

/**
 * @author Alexandre Russel
 */
public interface AnnotatedDocumentEventListener {

    String ANNOTATION_CREATED = "annotationCreated";

    String ANNOTATION_UPDATED = "annotationUpdated";

    String ANNOTATION_DELETED = "annotationDeleted";

    String ANNOTATION_ID = "annotationId";

    String ANNOTATION_SUBJECT = "annotationSubject";

    String ANNOTATION_BODY = "annotationBody";

    void beforeAnnotationCreated(NuxeoPrincipal principal, DocumentLocation documentLoc, Annotation annotation);

    void afterAnnotationCreated(NuxeoPrincipal principal, DocumentLocation documentLoc, Annotation annotation);

    void beforeAnnotationRead(NuxeoPrincipal principal, String annotationId);

    void afterAnnotationRead(NuxeoPrincipal principal, DocumentLocation documentLoc, Annotation annotation);

    void beforeAnnotationUpdated(NuxeoPrincipal principal, DocumentLocation documentLoc, Annotation annotation);

    void afterAnnotationUpdated(NuxeoPrincipal principal, DocumentLocation documentLoc, Annotation annotation);

    void beforeAnnotationDeleted(NuxeoPrincipal principal, DocumentLocation documentLoc, Annotation annotation);

    void afterAnnotationDeleted(NuxeoPrincipal principal, DocumentLocation documentLoc, Annotation annotation);
}
