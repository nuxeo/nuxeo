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

package org.nuxeo.ecm.platform.rendition.impl;

import java.util.List;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.rendition.Rendition;
import org.nuxeo.ecm.platform.rendition.RenditionException;
import org.nuxeo.ecm.platform.rendition.extension.RenditionProvider;
import org.nuxeo.ecm.platform.rendition.service.RenditionDefinition;

/**
 * Implementation of the {@link Rendition} interface that is applicable for
 * rendition created on the fly
 * 
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 * 
 */
public class LiveRendition extends LazyRendition implements Rendition {

    protected final DocumentModel doc;

    public LiveRendition(DocumentModel doc, RenditionDefinition definition) {
        super(definition);
        this.doc = doc;
    }

    @Override
    public boolean isStored() {
        return false;
    }

    @Override
    public DocumentModel getHostDocument() {
        return doc;
    }

    @Override
    protected List<Blob> computeRenditionBlobs() throws RenditionException {

        RenditionProvider provider = getDefinition().getProvider();
        if (provider == null) {
            throw new RenditionException("No Rendition provider defined");
        }

        return provider.render(getHostDocument(), getDefinition());
    }

}
