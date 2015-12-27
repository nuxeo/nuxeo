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
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.platform.rendition.Rendition;
import org.nuxeo.ecm.platform.rendition.extension.RenditionProvider;
import org.nuxeo.ecm.platform.rendition.service.RenditionDefinition;

/**
 * Implementation of the {@link Rendition} interface that is applicable for rendition created on the fly
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
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
    protected List<Blob> computeRenditionBlobs() {

        RenditionProvider provider = getDefinition().getProvider();
        if (provider == null) {
            throw new NuxeoException("No Rendition provider defined");
        }

        return provider.render(getHostDocument(), getDefinition());
    }

}
