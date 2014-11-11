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

package org.nuxeo.ecm.platform.annotations.service;

import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.annotations.api.Annotation;
import org.nuxeo.ecm.platform.annotations.repository.service.AnnotatedDocumentEventListener;

/**
 * @author Alexandre Russel
 *
 */
public class DummyDocumentEventListener implements
        AnnotatedDocumentEventListener {
    private static int test;

    public void afterAnnotationCreated(NuxeoPrincipal principal,
            DocumentLocation documentLoc, Annotation annotation) {
        test |= 1 << 1;
    }

    public void afterAnnotationDeleted(NuxeoPrincipal principal,
            DocumentLocation documentLoc, Annotation annotation) {
        test |= 1 << 7;
    }

    public void afterAnnotationRead(NuxeoPrincipal principal,
            DocumentLocation documentLoc, Annotation annotation) {
        test |= 1 << 3;

    }

    public void afterAnnotationUpdated(NuxeoPrincipal principal,
            DocumentLocation documentLoc, Annotation annotation) {
        test |= 1 << 5;
    }

    public void beforeAnnotationCreated(NuxeoPrincipal principal,
            DocumentLocation documentLoc, Annotation annotation) {
        test |= 1;
    }

    public void beforeAnnotationDeleted(NuxeoPrincipal principal,
            DocumentLocation documentLoc, Annotation annotation) {
        test |= 1 << 6;
    }

    public void beforeAnnotationRead(NuxeoPrincipal principal,
            String annotationId) {
        test |= 1 << 2;
    }

    public void beforeAnnotationUpdated(NuxeoPrincipal principal,
            DocumentLocation documentLoc, Annotation annotation) {
        test |= 1 << 4;
    }

    public static int getTest() {
        return test;
    }

    public static void resetTest() {
        test = 0;
    }
}
