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

import java.util.HashSet;
import java.util.Set;

import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.annotations.api.Annotation;
import org.nuxeo.ecm.platform.annotations.repository.service.AnnotatedDocumentEventListener;

/**
 * @author Alexandre Russel
 */
public class DummyDocumentEventListener implements AnnotatedDocumentEventListener {

    private static Set<String> test = new HashSet<>();

    @Override
    public void afterAnnotationCreated(NuxeoPrincipal principal, DocumentLocation documentLoc, Annotation annotation) {
        test.add("ac");
    }

    @Override
    public void afterAnnotationDeleted(NuxeoPrincipal principal, DocumentLocation documentLoc, Annotation annotation) {
        test.add("ad");
    }

    @Override
    public void afterAnnotationRead(NuxeoPrincipal principal, DocumentLocation documentLoc, Annotation annotation) {
        test.add("ar");
    }

    @Override
    public void afterAnnotationUpdated(NuxeoPrincipal principal, DocumentLocation documentLoc, Annotation annotation) {
        test.add("au");
    }

    @Override
    public void beforeAnnotationCreated(NuxeoPrincipal principal, DocumentLocation documentLoc, Annotation annotation) {
        test.add("bc");
    }

    @Override
    public void beforeAnnotationDeleted(NuxeoPrincipal principal, DocumentLocation documentLoc, Annotation annotation) {
        test.add("bd");
    }

    @Override
    public void beforeAnnotationRead(NuxeoPrincipal principal, String annotationId) {
        test.add("br");
    }

    @Override
    public void beforeAnnotationUpdated(NuxeoPrincipal principal, DocumentLocation documentLoc, Annotation annotation) {
        test.add("bu");
    }

    public static Set<String> getTest() {
        return test;
    }

    public static void resetTest() {
        test.clear();
    }

}
