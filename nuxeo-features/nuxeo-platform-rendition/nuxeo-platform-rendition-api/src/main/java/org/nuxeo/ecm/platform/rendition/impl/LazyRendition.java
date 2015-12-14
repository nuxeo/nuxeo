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
import org.nuxeo.ecm.platform.rendition.Rendition;
import org.nuxeo.ecm.platform.rendition.service.RenditionDefinition;

/**
 * Implementation of the {@link Rendition} interface that allows lazy computation of the rendition Blobs
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 */
public abstract class LazyRendition extends AbstractRendition implements Rendition {

    public static final String EMPTY_MARKER = "empty=true";

    public static final String ERROR_MARKER = "error=true";

    protected List<Blob> blobs = null;

    public LazyRendition(RenditionDefinition definition) {
        super(definition);
    }

    @Override
    public Blob getBlob() {
        List<Blob> blobs = getBlobs();
        if (blobs != null && blobs.size() > 0) {
            return blobs.get(0);
        }
        return null;
    }

    @Override
    public List<Blob> getBlobs() {
        if (blobs == null) {
            blobs = computeRenditionBlobs();
        }
        return blobs;
    }

    @Override
    public boolean isCompleted() {
        Blob blob = getBlob();
        if (blob == null) {
            return true;
        }
        String mimeType = blob.getMimeType();
        // lazy rendition with build in-progress has blob mimeType containing "empty=true"
        return mimeType == null || !mimeType.contains(EMPTY_MARKER);
    }

    protected abstract List<Blob> computeRenditionBlobs();

}
