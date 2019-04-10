/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     "<a href=\"mailto:bjalon@nuxeo.com\">Benjamin JALON</a>"
 */
package org.nuxeo.ecm.platform.media.streaming;

import java.io.File;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;

/**
 * @author "<a href=\"mailto:bjalon@nuxeo.com\">Benjamin JALON</a>"
 */
public class TestMediaStreamingUpdaterListener extends SQLRepositoryTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.core.event");
        deployBundle("org.nuxeo.ecm.core.convert.api");
        deployBundle("org.nuxeo.ecm.core.convert");
        deployBundle("org.nuxeo.ecm.platform.commandline.executor");
        deployBundle("org.nuxeo.ecm.platform.video.convert");
        deployBundle("org.nuxeo.ecm.platform.video.core");
        deployContrib("org.nuxeo.ecm.platform.video.core.test",
                "OSGI-INF/test-core-types-contrib.xml");

        openSession();
    }

    // public void testMediaUpdateInCreationWithServiceActivated() throws
    // Exception {
    // deployContrib("org.nuxeo.ecm.platform.video.core.test",
    // "OSGI-INF/test-streaming-contrib.xml");
    //
    // DocumentModel testDoc = session.createDocumentModel("/", "my-doc",
    // "TestStreamableDocument");
    // // needed to generate digests
    // testDoc.setPropertyValue("dc:modified", new GregorianCalendar());
    // FileBlob video = new FileBlob(new File(
    // this.getClass().getClassLoader().getResource(
    // "test-data/sample.mpg").toURI()));
    //
    // testDoc.setPropertyValue("file:content", video);
    // session.createDocument(testDoc);
    // session.save();
    //
    // testDoc = session.getDocument(testDoc.getRef());
    // Blob stream = (Blob)
    // testDoc.getPropertyValue(MediaStreamingConstants.STREAM_MEDIA_FIELD);
    // assertNotNull(stream);
    // assertTrue(((SQLBlob) stream).getFilename().endsWith("mp4"));
    //
    // }

    public void testMediaUpdateInCreationWithServiceNotActivated()
            throws Exception {
        deployContrib("org.nuxeo.ecm.platform.video.core.test",
                "OSGI-INF/test-streaming-desactivated-contrib.xml");

        DocumentModel testDoc = session.createDocumentModel("/", "my-doc",
                "TestStreamableDocument");
        FileBlob video = new FileBlob(new File(
                this.getClass().getClassLoader().getResource(
                        "test-data/sample.mpg").toURI()));

        testDoc.setPropertyValue("file:content", video);
        session.createDocument(testDoc);
        session.save();

        testDoc = session.getDocument(testDoc.getRef());
        assertNull(testDoc.getPropertyValue(MediaStreamingConstants.STREAM_MEDIA_FIELD));

    }

    public void testMediaUpdateInCreationWithBlobNull() throws Exception {
        deployContrib("org.nuxeo.ecm.platform.video.core.test",
                "OSGI-INF/test-streaming-desactivated-contrib.xml");

        DocumentModel testDoc = session.createDocumentModel("/", "my-doc",
                "TestStreamableDocument");
        session.createDocument(testDoc);
        session.save();

        testDoc = session.getDocument(testDoc.getRef());
        assertNull(testDoc.getPropertyValue(MediaStreamingConstants.STREAM_MEDIA_FIELD));

    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        closeSession();
    }

}
