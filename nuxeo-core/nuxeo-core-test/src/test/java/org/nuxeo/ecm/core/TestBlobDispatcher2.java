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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeFalse;

import java.io.InputStream;
import java.io.Serializable;

import javax.inject.Inject;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.blob.ManagedBlob;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.HotDeployer;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
public class TestBlobDispatcher2 {

    @Inject
    protected HotDeployer hotDeployer;

    @Inject
    protected CoreFeature coreFeature;

    @Inject
    protected CoreSession session;

    /**
     * Old blob which was created before dispatch was configured. It has no prefix but must still be dispatched
     * somewhere on read.
     */
    @Test
    public void testDispatchOldBlob() throws Exception {
        String foo = "foo";
        String foo_key = "acbd18db4cc2f85cedef654fccc4a4d8";

        // create a regular binary going to the default (non-dispatched) blob provider
        DocumentModel doc = session.createDocumentModel("/", "doc", "File");
        Blob blob = Blobs.createBlob(foo, "text/plain");
        doc.setPropertyValue("file:content", (Serializable) blob);
        doc = session.createDocument(doc);

        // check binary key
        blob = (Blob) doc.getPropertyValue("file:content");
        String key = ((ManagedBlob) blob).getKey();
        assertEquals(foo_key, key);

        // now install dispatch
        assumeFalse("Cannot test hot-deploy with in-memory repository",
                coreFeature.getStorageConfiguration().isDBSMem());
        hotDeployer.deploy("org.nuxeo.ecm.core.test.tests:OSGI-INF/test-blob-dispatcher.xml");
        // check that blob still readable
        doc = session.getDocument(doc.getRef());
        blob = (Blob) doc.getPropertyValue("file:content");
        try (InputStream in = blob.getStream()) {
            assertEquals("foo", IOUtils.toString(in, UTF_8));
        }
    }

}
