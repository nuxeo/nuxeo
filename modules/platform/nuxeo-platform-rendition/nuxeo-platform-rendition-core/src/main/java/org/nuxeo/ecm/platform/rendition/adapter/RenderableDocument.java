/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.platform.rendition.adapter;

import java.util.List;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.rendition.Renderable;
import org.nuxeo.ecm.platform.rendition.Rendition;
import org.nuxeo.ecm.platform.rendition.service.RenditionDefinition;
import org.nuxeo.ecm.platform.rendition.service.RenditionService;
import org.nuxeo.runtime.api.Framework;

/**
 * Default implementation for {@link Renderable} interface.
 * <p>
 * This is a simple wrapper around the {@link RenditionService} in the context of a given {@link DocumentModel}
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 */
public class RenderableDocument implements Renderable {

    protected final DocumentModel doc;

    protected List<RenditionDefinition> defs = null;

    public RenderableDocument(DocumentModel doc) {
        this.doc = doc;
    }

    @Override
    public List<RenditionDefinition> getAvailableRenditionDefinitions() {
        if (defs == null) {
            defs = Framework.getService(RenditionService.class).getAvailableRenditionDefinitions(doc);
        }
        return defs;
    }

    @Override
    public Rendition getRenditionByName(String name) {
        for (RenditionDefinition def : getAvailableRenditionDefinitions()) {
            if (def.getName().equals(name)) {
                return getRendition(def);
            }
        }
        return null;
    }

    @Override
    public Rendition getRenditionByKind(String kind) {
        for (RenditionDefinition def : getAvailableRenditionDefinitions()) {
            if (def.getKind().equals(kind)) {
                return getRendition(def);
            }
        }
        return null;
    }

    protected Rendition getRendition(RenditionDefinition def) {
        return Framework.getService(RenditionService.class).getRendition(doc, def.getName());

    }

}
