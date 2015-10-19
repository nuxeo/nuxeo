/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger
 */

package org.nuxeo.transientstore.test.work;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolderWithProperties;
import org.nuxeo.ecm.core.transientstore.work.TransientStoreWork;

/**
 * @since 7.4
 */
public class DummyTransientStoreWork extends TransientStoreWork {

    private static final long serialVersionUID = -8543577711553345800L;

    @Override
    public void work() {
        // store params and a blob
        Map<String, Serializable> params = new HashMap<>();
        params.put("firstparam", "firstvalue");
        params.put("secondparam", "secondvalue");

        Blob blob = Blobs.createBlob("a simple blob", "text/plain");

        BlobHolder bh = new SimpleBlobHolderWithProperties(blob, params);
        putBlobHolder(bh);
    }

    @Override
    public String getTitle() {
        return "Dummy transient store work";
    }
}
