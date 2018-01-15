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
 *     <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 */
package org.nuxeo.ecm.platform.rendition.service.lazy;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.platform.rendition.lazy.AbstractRenditionBuilderWork;
import org.nuxeo.ecm.platform.rendition.service.RenditionDefinition;

/**
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 * @since 7.2
 */
public class LazyRenditionWorkSample extends AbstractRenditionBuilderWork {

    private static final long serialVersionUID = 1L;

    public LazyRenditionWorkSample(String key, DocumentModel doc, RenditionDefinition def) {
        super(key, doc, def);
    }

    @Override
    protected List<Blob> doComputeRendition(CoreSession session, DocumentModel doc, RenditionDefinition def) {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
        List<Blob> blobs = new ArrayList<Blob>();
        StringBlob blob = new StringBlob("I am really lazy");
        blob.setFilename("LazyBoy.txt");
        blobs.add(blob);
        return blobs;
    }

}
