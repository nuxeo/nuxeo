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

import static org.nuxeo.ecm.core.io.registry.reflect.Instantiations.SINGLETON;
import static org.nuxeo.ecm.core.io.registry.reflect.Priorities.REFERENCE;

import java.io.IOException;
import java.net.URI;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.model.impl.primitives.BlobProperty;
import org.nuxeo.ecm.core.blob.BlobManager;
import org.nuxeo.ecm.core.blob.ManagedBlob;
import org.nuxeo.ecm.core.io.marshallers.json.enrichers.AbstractJsonEnricher;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;
import org.nuxeo.runtime.api.Framework;

import com.fasterxml.jackson.core.JsonGenerator;

/**
 * Enriches {@link Blob} json with embed preview link when available.
 * <p>
 * Enabled when parameter enrichers-blob=preview is present.
 * <p>
 * Blob format is:
 *
 * <pre>
 * {@code
 * {
 *  "name": "...",
 *  "mime-type": "...",
 *  ...
 *  "preview": "<url>"
 * }
 * }
 * </pre>
 *
 * @since 11.2
 */
@Setup(mode = SINGLETON, priority = REFERENCE)
public class BlobPreviewJsonEnricher extends AbstractJsonEnricher<BlobProperty> {

    public static final String NAME = "preview";

    public BlobPreviewJsonEnricher() {
        super(NAME);
    }

    @Override
    public void write(JsonGenerator jg, BlobProperty blobProperty) throws IOException {
        Blob blob = (Blob) blobProperty.getValue();
        if (!(blob instanceof ManagedBlob)) {
            return;
        }

        // if it's a managed blob try to use the embed uri for preview
        BlobManager blobManager = Framework.getService(BlobManager.class);
        URI uri = blobManager.getURI(blob, BlobManager.UsageHint.EMBED, null);
        if (uri != null) {
            jg.writeStringField(NAME, uri.toASCIIString());
        }
    }

}
