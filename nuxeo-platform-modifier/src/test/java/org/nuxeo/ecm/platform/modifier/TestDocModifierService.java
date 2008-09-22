/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.platform.modifier;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.api.impl.blob.ByteArrayBlob;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.model.Session;
import org.nuxeo.ecm.core.utils.DocumentModelUtils;
import org.nuxeo.ecm.platform.modifier.service.DocModifierService;
import org.nuxeo.ecm.platform.modifier.service.ServiceHelper;
import org.nuxeo.ecm.platform.transform.NXTransform;
import org.nuxeo.ecm.platform.transform.interfaces.Transformer;
import org.nuxeo.ecm.platform.transform.service.TransformService;
import org.nuxeo.runtime.api.Framework;


/**
 * Tests the Document Modifier component.
 *
 * @author <a href="mailto:dm@nuxeo.com">Dragos Mihalache</a>
 */
public class TestDocModifierService extends AbstractPluginTestCase {

    private static final Log log = LogFactory.getLog(TestDocModifierService.class);

    private Session session;

    private CoreSession coreSession;

    private Document root;

    private Transformer transformer;

    private static final String docPath_wordML = "src/test/resources/data/wordml/hello-simple.xml";

    @Override
    public void setUp() throws Exception {
        super.setUp();
        session = getRepository().getSession(null);
        root = session.getRootDocument();

        openCoreSession();

        TransformService service = (TransformService) Framework.getRuntime().getComponent(TransformService.NAME);
        transformer = service.getTransformerByName("any2text");
    }

    @Override
    public void tearDown() throws Exception {
        root.remove();
        session.save();
        // if (session != null)
        session.close();
        session = null;
        root = null;

        transformer = null;
        super.tearDown();
    }

    protected void openCoreSession() throws ClientException {
        Map<String, Serializable> context = new HashMap<String, Serializable>();
        context.put("username", "Administrator");
        coreSession = CoreInstance.getInstance().open("demo",
                context);
        assertNotNull(coreSession);
    }

    public void testDocumentModification() throws Exception {
        final String logPrefix = "<testDocumentModification> ";

        DocumentModel rootDM = coreSession.getRootDocument();

        DocumentModel testFile = new DocumentModelImpl(rootDM
                .getPathAsString(), "testfile1", "File");

        testFile = coreSession.createDocument(testFile);

        testFile.setProperty("dublincore", "description", "some file description");

        Map<String, Object> props = DocumentModelUtils.getProperties(testFile);
        log.info(logPrefix + "doc props: " + props.keySet());
        for (Map.Entry<String, Object> prop: props.entrySet()) {
            log.info(logPrefix + "prop : " + prop.getKey() + " = "
                    + prop.getValue());
        }

        testFile.setProperty("uid", "uid", "TEST0001");
        testFile.setProperty("uid", "major_version", 2L);
        testFile.setProperty("uid", "minor_version", 15L);

        final ByteArrayBlob content = getFileContent(docPath_wordML, "text/xml");
        testFile.setProperty("file", "content", content);
        testFile.setProperty("file", "filename", docPath_wordML);

        final DocModifierService service = ServiceHelper.getDocModifierService();

        service.processDocument(testFile, DocumentEventTypes.DOCUMENT_CREATED);

        final Blob newContent = (Blob) testFile.getProperty("file", "content");

        log.info(logPrefix + "new content: \n"
                + new String(newContent.getByteArray()));
    }

    public void testDocumentModificationWithOutput() throws Exception {
        final String logPrefix = "<testDocumentModificationWithOutput> ";

        DocumentModel rootDM = coreSession.getRootDocument();

        DocumentModel testFile = new DocumentModelImpl(rootDM
                .getPathAsString(), "testfile1", "File");

        testFile = coreSession.createDocument(testFile);

        testFile.setProperty("dublincore", "description", "some file description");

        Map<String, Object> props = DocumentModelUtils.getProperties(testFile);
        log.info(logPrefix + "doc props: " + props.keySet());
        for (Map.Entry<String, Object> prop: props.entrySet()) {
            log.info(logPrefix + "prop : " + prop.getKey() + " = "
                    + prop.getValue());
        }

        testFile.setProperty("uid", "uid", "TEST0001");
        testFile.setProperty("uid", "major_version", 2L);
        testFile.setProperty("uid", "minor_version", 15L);

        final ByteArrayBlob content = getFileContent(docPath_wordML, "text/xml");
        testFile.setProperty("file", "content", content);
        testFile.setProperty("file", "filename", docPath_wordML);

        // before transformation
        String description = (String) testFile.getProperty("dublincore", "description");
        assertEquals("some file description", description);

        final DocModifierService service = ServiceHelper.getDocModifierService();

        service.processDocument(testFile, DocumentEventTypes.DOCUMENT_CREATED);

        final Blob newContent = (Blob) testFile.getProperty("file", "content");

        log.info(logPrefix + "new content: \n"
                + new String(newContent.getByteArray()));

        String descriptionOut = (String) testFile.getProperty("dublincore", "description");

        // this is a value specified in the nxdocmodifier-test-contrib-bundle.xml
        assertEquals("test descr", descriptionOut);
    }

    public void z() throws Exception {
        final String logPrefix = "<testDocumentModificationOrder> ";

        DocumentModel rootDM = coreSession.getRootDocument();

        DocumentModel testFile = new DocumentModelImpl(rootDM
                .getPathAsString(), "testfile1", "File");

        testFile = coreSession.createDocument(testFile);

        testFile.setProperty("dublincore", "description", "some file description");

        Map<String, Object> props = DocumentModelUtils.getProperties(testFile);
        log.info(logPrefix + "doc props: " + props.keySet());
        for (Map.Entry<String, Object> prop: props.entrySet()) {
            log.info(logPrefix + "prop : " + prop.getKey() + " = "
                    + prop.getValue());
        }

        testFile.setProperty("uid", "uid", "TEST0001");
        testFile.setProperty("uid", "major_version", 2L);
        testFile.setProperty("uid", "minor_version", 15L);

        final ByteArrayBlob content = getFileContent(docPath_wordML, "text/xml");
        testFile.setProperty("file", "content", content);
        testFile.setProperty("file", "filename", docPath_wordML);

        // before transformation
        String description = (String) testFile.getProperty("dublincore", "description");
        assertEquals("some file description", description);

        final DocModifierService service = ServiceHelper.getDocModifierService();

        service.processDocument(testFile, DocumentEventTypes.DOCUMENT_CREATED);

        final Blob newContent = (Blob) testFile.getProperty("file", "content");

        assertNotNull(newContent);

        log.info(logPrefix + "new content: \n"
                + new String(newContent.getByteArray()));

        String titleOut = (String) testFile.getProperty("dublincore", "title");

        // this is a value specified in the nxdocmodifier-test-contrib-bundle.xml
        assertEquals("test title last", titleOut);
    }

}
