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
import java.io.IOException;
import java.util.GregorianCalendar;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.repository.RepositoryDescriptor;
import org.nuxeo.ecm.core.repository.RepositoryManager;
import org.nuxeo.ecm.core.repository.RepositoryService;
import org.nuxeo.ecm.core.storage.sql.DefaultBinaryManager;
import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;
import org.nuxeo.ecm.core.storage.sql.coremodel.SQLRepository;
import org.nuxeo.runtime.api.Framework;

public class TestMediaStreamingServiceImpl extends SQLRepositoryTestCase {

    protected static final Log log = LogFactory.getLog(TestMediaStreamingServiceImpl.class);

    DocumentRef docRef = null;

    MediaStreamingService service = null;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.platform.types.api");
        deployBundle("org.nuxeo.ecm.platform.types.core");
        deployBundle("org.nuxeo.ecm.core.convert.api");
        deployBundle("org.nuxeo.ecm.core.convert");
        deployBundle("org.nuxeo.ecm.platform.commandline.executor");
        deployBundle("org.nuxeo.ecm.platform.video.convert");
        deployBundle("org.nuxeo.ecm.platform.video.core");
        deployContrib("org.nuxeo.ecm.platform.video.core.test",
                "OSGI-INF/test-core-types-contrib.xml");

        openSession();

        service = Framework.getService(MediaStreamingService.class);
        assertNotNull(service);
        DocumentModel videoDoc = session.createDocumentModel("/",
                "test-document", "TestStreamableDocument");
        session.createDocument(videoDoc);
        session.save();
        docRef = videoDoc.getRef();
    }

    public void testGetVideoURLFromDocumentModelBlobNullServiceActivated()
            throws Exception {
        log.info("Test that streaming service activated, and stream-able blob field null return null");
        deployContrib("org.nuxeo.ecm.platform.video.core.test",
                "OSGI-INF/test-streaming-contrib.xml");
        assertTrue(service.isServiceActivated());
        DocumentModel videoDoc = session.getDocument(docRef);

        assertEquals(null, service.getStreamURLFromDocumentModel(videoDoc));
    }

    public void testGetVideoURLFromDocumentModelBlobNullServiceDesactivated()
            throws Exception {
        log.info("Test that empty field + streaming service deactivated return null URL");
        deployContrib("org.nuxeo.ecm.platform.video.core.test",
                "OSGI-INF/test-streaming-contrib.xml");
        assertTrue(service.isServiceActivated());
        deployContrib("org.nuxeo.ecm.platform.video.core.test",
                "OSGI-INF/test-streaming-desactivated-contrib.xml");
        assertTrue(!service.isServiceActivated());
        DocumentModel videoDoc = session.getDocument(docRef);

        assertEquals(null, service.getStreamURLFromDocumentModel(videoDoc));

    }

    public void testGetVideoURLFromDocumentModelBlobNotNullServiceDesactivated()
            throws Exception {
        log.info("Test that streaming service deactivated, url return is null");
        deployContrib("org.nuxeo.ecm.platform.video.core.test",
                "OSGI-INF/test-streaming-contrib.xml");
        assertTrue(service.isServiceActivated());
        deployContrib("org.nuxeo.ecm.platform.video.core.test",
                "OSGI-INF/test-streaming-desactivated-contrib.xml");
        assertTrue(!service.isServiceActivated());

        DocumentModel videoDoc = session.getDocument(docRef);
        FileBlob video = new FileBlob(new File(
                this.getClass().getClassLoader().getResource(
                        "test-data/sample.mpg").toURI()));

        videoDoc.setPropertyValue(MediaStreamingConstants.STREAM_MEDIA_FIELD,
                video);
        session.saveDocument(videoDoc);
        session.save();

        videoDoc = session.getDocument(videoDoc.getRef());
        assertEquals(null, service.getStreamURLFromDocumentModel(videoDoc));

    }

    public void testGetVideoURLFromDocumentModelBlobNotNullServiceActivated()
            throws Exception {
        log.info("Test that streaming service activated, and stream-able blob return the url to the streaming server");
        deployContrib("org.nuxeo.ecm.platform.video.core.test",
                "OSGI-INF/test-streaming-contrib.xml");
        assertTrue(service.isServiceActivated());
        DocumentModel videoDoc = session.getDocument(docRef);
        videoDoc.setPropertyValue("dc:modified", new GregorianCalendar());
        FileBlob video = new FileBlob(new File(
                this.getClass().getClassLoader().getResource(
                        "test-data/sample.mpg").toURI()));

        videoDoc.setPropertyValue("file:content", video);
        videoDoc.setPropertyValue(MediaStreamingConstants.STREAM_MEDIA_FIELD,
                video);
        session.saveDocument(videoDoc);
        session.save();

        videoDoc = session.getDocument(videoDoc.getRef());
        String streamingServerBaseURL = service.getStreamingServerBaseURL();
        String streamURL = service.getStreamURLFromDocumentModel(videoDoc);

        assertTrue(streamURL.startsWith(streamingServerBaseURL));

    }

    protected DefaultBinaryManager getBinaryManager(String repositoryName)
            throws IOException, Exception {
        RepositoryService repositoryService = (RepositoryService) Framework.getRuntime().getComponent(
                RepositoryService.NAME);
        RepositoryManager repositoryManager = repositoryService.getRepositoryManager();
        RepositoryDescriptor descriptor = repositoryManager.getDescriptor(repositoryName);
        DefaultBinaryManager binaryManager = new DefaultBinaryManager();
        binaryManager.initialize(SQLRepository.getDescriptor(descriptor));
        return binaryManager;
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        closeSession();
    }

}
