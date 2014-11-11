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

    void beforeAnnotationCreated(NuxeoPrincipal principal,
            DocumentLocation documentLoc, Annotation annotation);

    void afterAnnotationCreated(NuxeoPrincipal principal,
            DocumentLocation documentLoc, Annotation annotation);

    void beforeAnnotationRead(NuxeoPrincipal principal, String annotationId);

    void afterAnnotationRead(NuxeoPrincipal principal,
            DocumentLocation documentLoc, Annotation annotation);

    void beforeAnnotationUpdated(NuxeoPrincipal principal,
            DocumentLocation documentLoc, Annotation annotation);

    void afterAnnotationUpdated(NuxeoPrincipal principal,
            DocumentLocation documentLoc, Annotation annotation);

    void beforeAnnotationDeleted(NuxeoPrincipal principal,
            DocumentLocation documentLoc, Annotation annotation);

    void afterAnnotationDeleted(NuxeoPrincipal principal,
            DocumentLocation documentLoc, Annotation annotation);
}
