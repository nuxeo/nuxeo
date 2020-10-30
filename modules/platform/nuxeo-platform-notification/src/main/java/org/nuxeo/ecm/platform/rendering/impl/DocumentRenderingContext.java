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
 *     dragos
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.rendering.impl;

import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * Abstract RenderingContext subclass that adds capabilities of storing a DocumentModel and retrieve RenderingConfig
 * associated with the current set DocumentModel.
 *
 * @author <a href="mailto:dm@nuxeo.com">Dragos Mihalache</a>
 */
public class DocumentRenderingContext extends DefaultRenderingContext {

    private static final long serialVersionUID = 1626664478541223492L;

    public static final String CTX_PARAM_DOCUMENT = "doc";

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (adapter.isAssignableFrom(getClass())) {
            return adapter.cast(this);
        } else if (adapter.isAssignableFrom(DocumentModel.class)) {
            return adapter.cast(getDocument());
        }
        return null;
    }

    public void setDocument(DocumentModel doc) {
        put(CTX_PARAM_DOCUMENT, doc);
    }

    public DocumentModel getDocument() {
        return (DocumentModel) get(CTX_PARAM_DOCUMENT);
    }

}
