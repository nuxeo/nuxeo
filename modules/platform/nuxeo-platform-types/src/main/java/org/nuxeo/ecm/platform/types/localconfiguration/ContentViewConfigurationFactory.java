/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 * Contributors:
 * Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.platform.types.localconfiguration;

import static org.nuxeo.ecm.platform.types.localconfiguration.ContentViewConfigurationConstants.CONTENT_VIEW_CONFIGURATION_FACET;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.adapter.DocumentAdapterFactory;

/**
 * Factory creating the {@code ContentViewConfigurationAdapter} adapter if the document has the
 * {@code ContentViewLocalConfiguration} facet.
 *
 * @see ContentViewConfigurationAdapter
 * @author <a href="mailto:qlamerand@nuxeo.com">Quentin Lamernad</a>
 */
public class ContentViewConfigurationFactory implements DocumentAdapterFactory {

    @Override
    public Object getAdapter(DocumentModel doc, Class<?> itf) {
        if (doc.hasFacet(CONTENT_VIEW_CONFIGURATION_FACET)) {
            return new ContentViewConfigurationAdapter(doc);
        }
        return null;
    }

}
