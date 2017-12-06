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
 */
package org.nuxeo.ecm.platform.preview.adapter;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.adapter.DocumentAdapterFactory;
import org.nuxeo.runtime.api.Framework;

/**
 * Factory for the DocumentModelAdapter service.
 * <p>
 * Delegates the calls to a service dedicated to Preview Adapter that finds the right adapter implementation according
 * to document type and to registered custom adapters.
 *
 * @author tiry
 */
public class PreviewDocumentModelAdapterFactory implements DocumentAdapterFactory {

    protected static PreviewAdapterManager paManager;

    protected PreviewAdapterManager getPreviewAdapterManager() {
        if (paManager == null) {
            paManager = Framework.getService(PreviewAdapterManager.class);
        }
        return paManager;
    }

    public Object getAdapter(DocumentModel doc, Class itf) {
        return getPreviewAdapterManager().getAdapter(doc);
    }

}
