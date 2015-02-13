/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * Contributors:
 * Nuxeo - initial API and implementation
 */
package org.nuxeo.ecm.platform.rendition.adapter;

import java.util.List;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.rendition.Renderable;
import org.nuxeo.ecm.platform.rendition.Rendition;
import org.nuxeo.ecm.platform.rendition.RenditionException;
import org.nuxeo.ecm.platform.rendition.service.RenditionDefinition;
import org.nuxeo.ecm.platform.rendition.service.RenditionService;
import org.nuxeo.runtime.api.Framework;

/**
 * Default implementation for {@link Renderable} interface.
 * <p>
 * This is a simple wrapper around the {@link RenditionService} in the context
 * of a given {@link DocumentModel}
 * 
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 * 
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
            defs = Framework.getLocalService(RenditionService.class).getAvailableRenditionDefinitions(
                    doc);
        }
        return defs;
    }

    @Override
    public Rendition getRenditionByName(String name) throws RenditionException {
        for (RenditionDefinition def : getAvailableRenditionDefinitions()) {
            if (def.getName().equals(name)) {
                return getRendition(def);
            }
        }
        return null;
    }

    @Override
    public Rendition getRenditionByKind(String kind) throws RenditionException {
        for (RenditionDefinition def : getAvailableRenditionDefinitions()) {
            if (def.getKind().equals(kind)) {
                return getRendition(def);
            }
        }
        return null;
    }

    protected Rendition getRendition(RenditionDefinition def)
            throws RenditionException {
        return Framework.getLocalService(RenditionService.class).getRendition(
                doc, def.getName());

    }

}
