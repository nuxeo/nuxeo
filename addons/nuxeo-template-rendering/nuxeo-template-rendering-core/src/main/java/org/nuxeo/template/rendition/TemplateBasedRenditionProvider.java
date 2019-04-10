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
 *     Estelle Giuly <egiuly@nuxeo.com>
 */
package org.nuxeo.template.rendition;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.rendition.extension.RenditionProvider;
import org.nuxeo.ecm.platform.rendition.service.RenditionDefinition;
import org.nuxeo.template.api.adapters.TemplateBasedDocument;

public class TemplateBasedRenditionProvider implements RenditionProvider {

    protected static Log log = LogFactory.getLog(TemplateBasedRenditionProvider.class);

    @Override
    public boolean isAvailable(DocumentModel doc, RenditionDefinition def) {
        TemplateBasedDocument tbd = doc.getAdapter(TemplateBasedDocument.class);
        if (tbd != null) {
            // check if some template has been bound to a rendition
            String template = tbd.getTemplateNameForRendition(def.getName());
            return template == null ? false : true;
        }
        return false;
    }

    @Override
    public List<Blob> render(DocumentModel doc, RenditionDefinition definition) {
        TemplateBasedDocument tbd = doc.getAdapter(TemplateBasedDocument.class);
        String template = tbd.getTemplateNameForRendition(definition.getName());
        List<Blob> blobs = new ArrayList<Blob>();
        if (template != null) {
            Blob rendered = tbd.renderWithTemplate(template);
            blobs.add(rendered);
        }
        return blobs;
    }

}
