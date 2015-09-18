/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.ecm.automation.server.jaxrs.batch;

import java.util.Collections;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.transientstore.AbstractStorageEntry;
import org.nuxeo.ecm.core.transientstore.api.TransientStore;

/**
 * Represents a chunk from a batch file backed by the {@link TransientStore}.
 *
 * @since 7.4
 * @see BatchFileEntry
 * @see Batch
 */
public class BatchChunkEntry extends AbstractStorageEntry {

    private static final long serialVersionUID = 1L;

    public BatchChunkEntry(String id, Blob blob) {
        super(id);
        setBlobs(Collections.singletonList(blob));
    }

    public Blob getBlob() {
        List<Blob> blobs = getBlobs();
        if (CollectionUtils.isEmpty(blobs)) {
            return null;
        }
        return blobs.get(0);
    }

    @Override
    public void beforeRemove() {
        // Nothing to do here
    }
}
