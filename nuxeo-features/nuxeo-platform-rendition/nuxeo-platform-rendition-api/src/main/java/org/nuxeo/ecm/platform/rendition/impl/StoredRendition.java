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
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.platform.rendition.Rendition;
import org.nuxeo.ecm.platform.rendition.service.RenditionDefinition;

/**
 * Implementation of the {@link Rendition} interface for rendition that are stored in the Repository
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 */
public class StoredRendition extends AbstractRendition implements Rendition {

    protected final DocumentModel stored;

    public StoredRendition(DocumentModel stored, RenditionDefinition definition) {
        super(definition);
        this.stored = stored;
    }

    @Override
    public boolean isStored() {
        return true;
    }

    @Override
    public boolean isCompleted() {
        return true;
    }

    @Override
    public Blob getBlob() {
        return stored.getAdapter(BlobHolder.class).getBlob();
    }

    @Override
    public List<Blob> getBlobs() {
        return stored.getAdapter(BlobHolder.class).getBlobs();
    }

    @Override
    public DocumentModel getHostDocument() {
        return stored;
    }

}
