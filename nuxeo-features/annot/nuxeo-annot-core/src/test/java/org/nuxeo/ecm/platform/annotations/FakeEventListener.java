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

package org.nuxeo.ecm.platform.annotations;

import java.security.Principal;

import org.nuxeo.ecm.platform.annotations.api.Annotation;
import org.nuxeo.ecm.platform.annotations.service.EventListener;

/**
 * @author Alexandre Russel
 */
public class FakeEventListener implements EventListener {

    public void afterAnnotationCreated(Principal principal, Annotation annotation) {
    }

    public void afterAnnotationDeleted(Principal principal, Annotation annotation) {
    }

    public void afterAnnotationRead(Principal principal, Annotation annotation) {
    }

    public void afterAnnotationUpdated(Principal principal, Annotation annotation) {
    }

    public void beforeAnnotationCreated(Principal principal, Annotation annotation) {
    }

    public void beforeAnnotationDeleted(Principal principal, Annotation annotation) {
    }

    public void beforeAnnotationRead(Principal principal, String annotationId) {
    }

    public void beforeAnnotationUpdated(Principal principal, Annotation annotation) {
    }

}
