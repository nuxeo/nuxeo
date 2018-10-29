/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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

package org.nuxeo.ecm.core.io.marshallers.json.enrichers;

import com.fasterxml.jackson.core.JsonGenerator;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.blob.BlobManager;
import org.nuxeo.ecm.core.blob.BlobProvider;
import org.nuxeo.ecm.core.blob.ManagedBlob;
import org.nuxeo.ecm.core.blob.apps.AppLink;
import org.nuxeo.ecm.core.io.marshallers.json.document.DocumentModelJsonWriter;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;
import org.nuxeo.runtime.api.Framework;

import java.io.IOException;
import java.util.List;

import static org.nuxeo.ecm.core.io.registry.reflect.Instantiations.SINGLETON;
import static org.nuxeo.ecm.core.io.registry.reflect.Priorities.REFERENCE;

/**
 * Enrich {@link Blob} json with list of {@link AppLink}.
 *
 * Enabled when parameter enrichers-blob=appLinks is present.
 *
 * <p>
 * Blob format is:
 *
 * <pre>{@code
 * {
 *  "name": "...",
 *  "mime-type": "...",
 *  ...
 *  "appLinks": [
 *    {
 *      "appName": "...",
 *      "icon": "...",
 *      "link": "..."
 *    },
 *    ...
 *  ]
 * }
 * }</pre>
 *
 * @since 10.3
 */
@Setup(mode = SINGLETON, priority = REFERENCE)
public class BlobAppLinksJsonEnricher extends AbstractJsonEnricher<Blob> {

    public static final String NAME = "appLinks";

    public BlobAppLinksJsonEnricher() {
        super(NAME);
    }

    @Override
    public void write(JsonGenerator jg, Blob blob) throws IOException {
        if (!(blob instanceof ManagedBlob)) {
            return;
        }

        ManagedBlob managedBlob = (ManagedBlob) blob;

        BlobManager blobManager = Framework.getService(BlobManager.class);
        BlobProvider blobProvider = blobManager.getBlobProvider(managedBlob.getProviderId());
        if (blobProvider == null) {
            return;
        }

        DocumentModel doc = ctx.getParameter(DocumentModelJsonWriter.ENTITY_TYPE);
        if (doc == null) {
            return;
        }

        jg.writeFieldName(NAME);
        jg.writeStartArray();
        try (RenderingContext.SessionWrapper wrapper = ctx.getSession(doc)) {
            NuxeoPrincipal principal = wrapper.getSession().getPrincipal();
            if (principal != null) {
                List<AppLink> apps = blobProvider.getAppLinks(principal.getName(), managedBlob);
                for (AppLink app : apps) {
                    jg.writeObject(app);
                }
            }
        }
        jg.writeEndArray();
    }

}
