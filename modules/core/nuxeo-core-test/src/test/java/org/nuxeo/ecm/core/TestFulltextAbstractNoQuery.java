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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.nuxeo.ecm.core.api.CoreSession.BINARY_FULLTEXT_MAIN_KEY;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.query.QueryParseException;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy("org.nuxeo.ecm.core.test.tests:OSGI-INF/disable-schedulers.xml")
public abstract class TestFulltextAbstractNoQuery {

    protected static final String CONTENT = "hello world";

    protected static final String BINARY_TEXT = " hello world ";

    protected static final String BINARY_TEXT_MD5 = "c9708cb9a6b8d820eda83547a218f384";

    @Inject
    protected CoreFeature coreFeature;

    @Inject
    protected CoreSession session;

    @Test
    public void testBinaryText() throws IOException { // NOSONAR (subclass throws)
        DocumentModel doc = session.createDocumentModel("/", "doc", "File");
        doc.setPropertyValue("file:content", (Serializable) Blobs.createBlob(CONTENT));
        doc = session.createDocument(doc);
        session.save();
        coreFeature.waitForAsyncCompletion();

        // check whether or not we can get the binary text
        Map<String, String> map = session.getBinaryFulltext(doc.getRef());
        String fulltext = map == null ? null : map.get(BINARY_FULLTEXT_MAIN_KEY);
        if (expectBinaryText()) {
            assertEquals(BINARY_TEXT, fulltext);
        } else {
            assertNull(fulltext);
        }

        // check that repository-based fulltext search fails
        try {
            String nxql = "SELECT * FROM Document WHERE ecm:fulltext = 'hello'";
            session.query(nxql);
            fail("fulltext query should have failed");
        } catch (QueryParseException e) {
            String message = e.getMessage();
            assertTrue(message, message.contains("Fulltext search disabled by configuration"));
        }
    }

    protected abstract boolean expectBinaryText();

}
