/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
