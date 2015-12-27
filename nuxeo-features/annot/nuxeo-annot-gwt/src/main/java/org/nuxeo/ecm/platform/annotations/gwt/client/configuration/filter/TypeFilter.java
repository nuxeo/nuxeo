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
 *     troger
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.annotations.gwt.client.configuration.filter;

import org.nuxeo.ecm.platform.annotations.gwt.client.configuration.AnnotationDefinition;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
public class TypeFilter implements AnnotationDefinitionFilter {

    private final String type;

    private final boolean accept;

    public TypeFilter(String type) {
        this.type = type;
        this.accept = true;
    }

    public TypeFilter(String type, boolean accept) {
        this.type = type;
        this.accept = accept;
    }

    public boolean accept(AnnotationDefinition annotationDefinition) {
        if (accept) {
            return type.equals(annotationDefinition.getType());
        } else {
            return !type.equals(annotationDefinition.getType());
        }
    }

}
