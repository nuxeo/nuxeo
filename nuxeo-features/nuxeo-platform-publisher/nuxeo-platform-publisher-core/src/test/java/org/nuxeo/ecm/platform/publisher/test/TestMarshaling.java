/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo
 */

package org.nuxeo.ecm.platform.publisher.test;

import org.junit.Test;
import static org.junit.Assert.*;

import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.platform.publisher.api.PublishedDocument;
import org.nuxeo.ecm.platform.publisher.remoting.marshaling.DefaultPublishedDocumentMarshaler;
import org.nuxeo.ecm.platform.publisher.remoting.marshaling.basic.BasicPublishedDocument;
import org.nuxeo.ecm.platform.publisher.remoting.marshaling.interfaces.PublishedDocumentMarshaler;

/**
 * Test simple marshaling
 *
 * @author tiry
 */
public class TestMarshaling {

    @Test
    public void testPublishedDocMarshaling() {
        PublishedDocument pubDoc = new BasicPublishedDocument(new IdRef("1"), "demo", "local", "1.0", "path",
                "parentPath", true);

        PublishedDocumentMarshaler marshaler = new DefaultPublishedDocumentMarshaler();

        String data = marshaler.marshalPublishedDocument(pubDoc);

        assertNotNull(data);
        // System.out.print(data);

        PublishedDocument pubDoc2 = marshaler.unMarshalPublishedDocument(data);
        assertNotNull(pubDoc2);

        assertEquals(pubDoc.getSourceDocumentRef(), pubDoc2.getSourceDocumentRef());
        assertEquals(pubDoc.getSourceRepositoryName(), pubDoc2.getSourceRepositoryName());
        assertEquals(pubDoc.getSourceServer(), pubDoc2.getSourceServer());
        assertEquals(pubDoc.getSourceVersionLabel(), pubDoc2.getSourceVersionLabel());
        assertEquals(pubDoc.getPath(), pubDoc2.getPath());
        assertEquals(pubDoc.getParentPath(), pubDoc2.getParentPath());
        assertEquals(pubDoc.isPending(), pubDoc2.isPending());
    }

}
