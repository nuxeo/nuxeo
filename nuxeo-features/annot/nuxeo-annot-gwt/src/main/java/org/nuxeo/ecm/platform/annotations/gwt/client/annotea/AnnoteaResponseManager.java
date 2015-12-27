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

package org.nuxeo.ecm.platform.annotations.gwt.client.annotea;

import java.util.List;

import org.nuxeo.ecm.platform.annotations.gwt.client.controler.AnnotationController;
import org.nuxeo.ecm.platform.annotations.gwt.client.model.Annotation;

/**
 * @author Alexandre Russel
 */
public class AnnoteaResponseManager {
    private static AnnotationController controller;

    private final RDFParser parser;

    public AnnoteaResponseManager(AnnotationController annotationController) {
        controller = annotationController;
        parser = new RDFParser();
    }

    public void processSubmitAnnotationResponse(String response) {
    }

    public void processAnnotationListResponse(String response) {
        List<Annotation> annotations = parser.getAnnotationList(response);
        for (int x = 0; x < annotations.size(); x++) {
            Annotation annotation = annotations.get(x);
            annotation.getXpointer();
            annotation.setId(x);
        }

        controller.setAnnotationList(annotations);
    }

}
