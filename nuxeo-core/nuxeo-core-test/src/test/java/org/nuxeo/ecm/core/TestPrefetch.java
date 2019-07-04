/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
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

package org.nuxeo.ecm.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Set;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.schema.PrefetchInfo;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

/**
 * @since 11.1
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
public class TestPrefetch {

    @Inject
    protected CoreSession session;

    @Inject
    protected TransactionalFeature txFeature;

    @Test
    public void testPrefetchInfoParsing() {
        String expression = "common.icon, file, uid, dc:title, dc:description, dc:modified,\n dc:lastContributor";
        PrefetchInfo prefetchInfo = new PrefetchInfo(expression);

        Set<String> expectedFields = Set.of("icon", "dc:title", "dc:description", "dc:modified", "dc:lastContributor");
        assertEquals(expectedFields, Set.of(prefetchInfo.getFields()));

        Set<String> expectedSchemas = Set.of("common", "dublincore", "file", "uid");
        assertEquals(expectedSchemas, Set.of(prefetchInfo.getSchemas()));
    }

    @Test
    public void testDocumentModelPrefetch() {
        DocumentModel doc = session.createDocumentModel("/", "foo", "File");
        doc.setPropertyValue("dc:title", "foo/title");
        doc.setPropertyValue("dc:description", "foo/description");
        doc.setPropertyValue("common:icon", "foo/icon");
        doc.setPropertyValue("uid:uid", "foo/uid"); // not prefetched
        doc = session.createDocument(doc);

        // make sure we are in a new clean transaction
        txFeature.nextTransaction();

        doc = session.getDocument(doc.getRef());
        doc.detach(false);

        assertEquals("foo/title", doc.getPropertyValue("dc:title"));
        assertEquals("foo/description", doc.getPropertyValue("dc:description"));
        assertEquals("foo/icon", doc.getPropertyValue("common:icon"));
        assertNull(doc.getPropertyValue("uid:uid"));
    }
}
