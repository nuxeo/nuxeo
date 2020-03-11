/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Bogdan Stefanescu
 */
package org.nuxeo.ecm.core.test;

import static org.junit.Assert.assertEquals;

import java.io.Serializable;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(init = DefaultRepositoryInit.class)
public class DocumentPropertyTest {

    @Inject
    public CoreSession session;

    @Test
    public void theSessionIsUsable() throws Exception {
        DocumentModel doc = session.createDocumentModel("/default-domain/workspaces", "myfile", "File");
        Blob blob = Blobs.createBlob("test");
        blob.setFilename("myfile");
        blob.setDigest("mydigest");
        doc.setPropertyValue("file:content", (Serializable) blob);
        doc = session.createDocument(doc);
        doc = session.getDocument(doc.getRef());
        assertEquals("myfile", doc.getPropertyValue("file:content/name"));
        assertEquals("mydigest", doc.getPropertyValue("file:content/digest"));
    }

}
