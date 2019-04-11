/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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

package org.nuxeo.ecm.core.api.adapter;

import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class AnnotatedDocumentAdapter implements AnnotatedDocument {

    final DocumentModel doc;

    final Map<String, Object> annotations;

    public AnnotatedDocumentAdapter(DocumentModel doc) {
        this.doc = doc;
        // initialize adapter -> in real cases you may get a proxy to a remote service
        annotations = new HashMap<>();
    }

    @Override
    public Object getAnnotation(String name) {
        return annotations.get(name);
    }

    @Override
    public void putAnnotation(String name, Object value) {
        annotations.put(name, value);
    }

}
