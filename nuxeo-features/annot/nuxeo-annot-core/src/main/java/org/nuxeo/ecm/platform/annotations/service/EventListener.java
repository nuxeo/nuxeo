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

package org.nuxeo.ecm.platform.annotations.service;

import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.annotations.api.Annotation;

/**
 * @author Alexandre Russel
 */
public interface EventListener {

    void beforeAnnotationCreated(NuxeoPrincipal principal, Annotation annotation);

    void afterAnnotationCreated(NuxeoPrincipal principal, Annotation annotation);

    void beforeAnnotationRead(NuxeoPrincipal principal, String annotationId);

    void afterAnnotationRead(NuxeoPrincipal principal, Annotation annotation);

    void beforeAnnotationUpdated(NuxeoPrincipal principal, Annotation annotation);

    void afterAnnotationUpdated(NuxeoPrincipal principal, Annotation annotation);

    void beforeAnnotationDeleted(NuxeoPrincipal principal, Annotation annotation);

    void afterAnnotationDeleted(NuxeoPrincipal principal, Annotation annotation);
}
