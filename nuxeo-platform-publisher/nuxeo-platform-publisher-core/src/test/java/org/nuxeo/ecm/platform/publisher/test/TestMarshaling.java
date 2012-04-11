/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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
import org.nuxeo.ecm.platform.publisher.remoting.marshaling.interfaces.PublishingMarshalingException;

/**
 *
 * Test simple marshaling
 *
 * @author tiry
 *
 */
public class TestMarshaling {

    @Test
    public void testPublishedDocMarshaling()
            throws PublishingMarshalingException {
        PublishedDocument pubDoc = new BasicPublishedDocument(new IdRef("1"),
                "demo", "local", "1.0", "path", "parentPath", true);

        PublishedDocumentMarshaler marshaler = new DefaultPublishedDocumentMarshaler();

        String data = marshaler.marshalPublishedDocument(pubDoc);

        assertNotNull(data);
        // System.out.print(data);

        PublishedDocument pubDoc2 = marshaler.unMarshalPublishedDocument(data);
        assertNotNull(pubDoc2);

        assertEquals(pubDoc.getSourceDocumentRef(),
                pubDoc2.getSourceDocumentRef());
        assertEquals(pubDoc.getSourceRepositoryName(),
                pubDoc2.getSourceRepositoryName());
        assertEquals(pubDoc.getSourceServer(), pubDoc2.getSourceServer());
        assertEquals(pubDoc.getSourceVersionLabel(),
                pubDoc2.getSourceVersionLabel());
        assertEquals(pubDoc.getPath(), pubDoc2.getPath());
        assertEquals(pubDoc.getParentPath(), pubDoc2.getParentPath());
        assertEquals(pubDoc.isPending(), pubDoc2.isPending());
    }

}
