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
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.api.impl.blob.ByteArrayBlob;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.model.Session;
import org.nuxeo.ecm.platform.transform.NXTransform;
import org.nuxeo.ecm.platform.transform.document.TransformDocumentImpl;
import org.nuxeo.ecm.platform.transform.interfaces.TransformDocument;
import org.nuxeo.ecm.platform.transform.interfaces.Transformer;
import org.nuxeo.ecm.platform.transform.service.TransformService;

/**
 * Test the Document Modifier component.
 *
 * @author DM
 *
 */
public class TestModifierOnDocModel extends AbstractPluginTestCase {

    private static final Log log = LogFactory.getLog(TestModifierOnDocModel.class);

    private Session session;

    private Document root;

    private CoreSession coreSession;

    private Transformer transformer;

    private static final String docPath_MSWORD = "test/resources/data/SampleDoc.doc";

    private static final String docPath_wordML = "src/test/resources/data/wordml/descr04_appro.xml";

    @Override
    public void setUp() throws Exception {
        super.setUp();
        session = getRepository().getSession(null);
        root = session.getRootDocument();

        openCoreSession();

        TransformService service = NXTransform.getTransformService();
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
        coreSession = CoreInstance.getInstance().open("demo", context);
        assertNotNull(coreSession);
    }

    /*
     * private void createDoc() { Document folder1 =
     * root.addChild("testfolder1", "Folder"); // create Doc file 1 Document
     * file1 = folder1.addChild("testfile1", "File");
     * file1.setString("dc:title", "testfile1_Title");
     * file1.setString("dc:description", "testfile1_description");
     *
     * File file = new File(docPath); try { final byte[] docContent =
     * FileUtils.getBytesFromFile(file);
     *
     * ByteArrayBlob fileContent = new ByteArrayBlob(docContent,
     * "application/msword"); // XXX: this will block the search: // FIXME:
     * indexing?? file1.setPropertyValue("content", fileContent); } catch
     * (IOException e) { log.error("Error reading file: " +
     * file.getAbsolutePath()); e.printStackTrace(); // FIXME: fail(...); }
     * file1.setPropertyValue("filename", file.getName()); }
     */

    public void testDocumentModification() throws Exception {

        // create a document model containing a file with fields to be replaced
        // with doc metadata
        DocumentModel rootDM = coreSession.getRootDocument();

        DocumentModel childFile = new DocumentModelImpl(
                rootDM.getPathAsString(), "testfile1", "File");
        childFile = coreSession.createDocument(childFile);

        childFile.setProperty("uid", "uid", "TEST0001");
        childFile.setProperty("uid", "major_version", 2L);
        childFile.setProperty("uid", "minor_version", 15L);

        final ByteArrayBlob content = getFileContent(docPath_wordML, "text/xml");
        childFile.setProperty("file", "content", content);
        childFile.setProperty("file", "filename", docPath_wordML);

        // should fill datamodel

        coreSession.saveDocument(childFile);
        // coreSession.save();
        // assertEquals(2L, childFile.getProperty("uid", "major_version"));
    }

    public void __testDirectModification() throws Exception {
        final ByteArrayBlob content = getFileContent(docPath_wordML, "text/xml");
        modifyContent(content);
    }

    private void modifyContent(final ByteArrayBlob content) {
        Map<String, Map<String, Serializable>> options = new HashMap<String, Map<String, Serializable>>();
        // options.put(pluginName, pluginOptions);

        final TransformDocument transformDocument = new TransformDocumentImpl(
                content);

        List<TransformDocument> results = transformer.transform(options,
                transformDocument);
    }

    public void __testPDF2textConvertion() {

        String path = "test-data/hello.pdf";
        /*
         * SerializableInputStream sstream =
         * getSerializableInputStreamFromPath(path); List<TransformDocument>
         * results = transformer.transform(null, new
         * TransformDocumentImpl(sstream, "application/pdf"));
         *
         * File textFile = getFileFromInputStream(results.get(0).getStream(),
         * "txt"); textFile.deleteOnExit(); assertEquals("text content", "Hello
         * from a PDF Document!", DocumentTestUtils.readContent(textFile));
         *
         * sstream.close();
         */
    }
}
