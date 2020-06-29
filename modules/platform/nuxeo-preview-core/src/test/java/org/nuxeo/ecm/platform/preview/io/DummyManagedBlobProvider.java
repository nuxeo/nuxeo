/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
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
 *     Nelson Silva <nsilva@nuxeo.com>
 */

package org.nuxeo.ecm.platform.preview.io;

import java.net.URI;

import javax.servlet.http.HttpServletRequest;

import org.nuxeo.ecm.core.blob.BlobManager;
import org.nuxeo.ecm.core.blob.DummyBlobProvider;
import org.nuxeo.ecm.core.blob.ManagedBlob;

/**
 * Dummy storage in memory.
 */
public class DummyManagedBlobProvider extends DummyBlobProvider {

    public static String getBlobKey(ManagedBlob blob) {
        String key = blob.getKey();
        int colon = key.indexOf(':');
        return colon < 0 ? key : key.substring(colon + 1);
    }

    @Override
    public URI getURI(ManagedBlob blob, BlobManager.UsageHint usage, HttpServletRequest servletRequest) {
        return URI.create("http://example.com/" + getBlobKey(blob) + "/" + usage.name().toLowerCase());
    }
}
