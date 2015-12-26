/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thierry Delprat
 */
package org.nuxeo.ecm.platform.rendition.extension;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.rendition.service.RenditionDefinition;

public class DefaultAutomationRenditionProvider implements RenditionProvider {

    protected static final Log log = LogFactory.getLog(DefaultAutomationRenditionProvider.class);

    @Override
    public boolean isAvailable(DocumentModel doc, RenditionDefinition def) {
        return AutomationRenderer.isRenditionAvailable(doc, def);
    }

    @Override
    public List<Blob> render(DocumentModel doc, RenditionDefinition definition) {
        return AutomationRenderer.render(doc, definition, null);
    }

    @Override
    public String getVariant(DocumentModel doc, RenditionDefinition definition) {
        return AutomationRenderer.getVariant(doc, definition);
    }

}
