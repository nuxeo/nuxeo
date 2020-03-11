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

    public static final String STALE_MARKER = "stale=true";

    public static final String IN_PROGRESS_MARKER = "inprogress";

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
