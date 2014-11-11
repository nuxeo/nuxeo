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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;

import org.apache.commons.io.FilenameUtils;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.impl.DocumentLocationImpl;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;
import org.nuxeo.ecm.platform.publisher.api.AbstractBasePublicationTree;
import org.nuxeo.ecm.platform.publisher.api.PublicationNode;
import org.nuxeo.ecm.platform.publisher.api.PublicationTree;
import org.nuxeo.ecm.platform.publisher.api.PublishedDocument;
import org.nuxeo.ecm.platform.publisher.api.PublisherService;
import org.nuxeo.ecm.platform.publisher.descriptors.PublicationTreeConfigDescriptor;
import org.nuxeo.ecm.platform.publisher.impl.localfs.LocalFSPublicationTree;
import org.nuxeo.ecm.platform.publisher.impl.service.PublisherServiceImpl;
import org.nuxeo.runtime.api.Framework;

public class TestFSPublishing extends SQLRepositoryTestCase {

    protected DocumentModel doc2Publish;

    protected DocumentLocation doc2publishLocation;

    protected File rootFolder;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.platform.content.template");
        deployBundle("org.nuxeo.ecm.platform.types.api");
        deployBundle("org.nuxeo.ecm.platform.types.core");
        deployBundle("org.nuxeo.ecm.platform.versioning.api");
        deployBundle("org.nuxeo.ecm.platform.versioning");
        deployBundle("org.nuxeo.ecm.platform.query.api");


        deployBundle("org.nuxeo.ecm.platform.publisher.core.contrib");
        deployBundle("org.nuxeo.ecm.platform.publisher.core");

        fireFrameworkStarted();
        openSession();
    }

    @Override
    @After
    public void tearDown() throws Exception {
        closeSession();
        super.tearDown();
    }

    protected void createInitialDocs() throws Exception {

        DocumentModel wsRoot = session.getDocument(new PathRef(
                "default-domain/workspaces"));

        DocumentModel ws = session.createDocumentModel(
                wsRoot.getPathAsString(), "ws1", "Workspace");
        ws.setProperty("dublincore", "title", "test WS");
        ws = session.createDocument(ws);

        DocumentModel sectionsRoot = session.getDocument(new PathRef(
                "default-domain/sections"));

        DocumentModel section1 = session.createDocumentModel(
                sectionsRoot.getPathAsString(), "section1", "Section");
        section1.setProperty("dublincore", "title", "section1");
        section1 = session.createDocument(section1);

        DocumentModel section2 = session.createDocumentModel(
                sectionsRoot.getPathAsString(), "section2", "Section");
        section2.setProperty("dublincore", "title", "section2");
        section2 = session.createDocument(section2);

        DocumentModel section11 = session.createDocumentModel(
                section1.getPathAsString(), "section11", "Section");
        section11.setProperty("dublincore", "title", "section11");
        section11 = session.createDocument(section11);

        doc2Publish = session.createDocumentModel(ws.getPathAsString(), "file",
                "File");
        doc2Publish.setProperty("dublincore", "title", "MyDoc");

        Blob blob = new StringBlob("SomeDummyContent");
        blob.setFilename("dummyBlob.txt");
        blob.setMimeType("text/plain");
        doc2Publish.setProperty("file", "content", blob);

        doc2Publish = session.createDocument(doc2Publish);

        session.save();

        doc2publishLocation = new DocumentLocationImpl(doc2Publish);
    }

    protected void createFSDirs() {
        String tmpPath = new Path(System.getProperty("java.io.tmpdir")).append(
                "TestFSSections" + System.currentTimeMillis()).toString();

        rootFolder = new File(tmpPath);
        rootFolder.mkdirs();

        new File(
                new Path(rootFolder.getAbsolutePath()).append("section1").toString()).mkdirs();
        File section2 = new File(new Path(rootFolder.getAbsolutePath()).append(
                "section2").toString());
        section2.mkdirs();
        new File(
                new Path(rootFolder.getAbsolutePath()).append("section3").toString()).mkdirs();

        new File(
                new Path(section2.getAbsolutePath()).append("section21").toString()).mkdirs();
        new File(
                new Path(section2.getAbsolutePath()).append("section22").toString()).mkdirs();
    }

    @Test
    public void testFSPublishing() throws Exception {
        createInitialDocs();
        createFSDirs();

        registerFSTree("TestingFSTree");

        PublisherService service = Framework.getLocalService(PublisherService.class);
        PublicationTree tree = service.getPublicationTree("TestingFSTree",
                session, null);

        // check browsing
        List<PublicationNode> sectionsNodes = tree.getChildrenNodes();
        assertNotNull(sectionsNodes);
        assertEquals(3, sectionsNodes.size());

        PublicationNode section2 = sectionsNodes.get(1);
        assertEquals("section2", section2.getName());
        assertEquals(
                FilenameUtils.separatorsToSystem(rootFolder + "/section2"),
                FilenameUtils.separatorsToSystem(section2.getPath()));

        List<PublicationNode> subSectionsNodes = section2.getChildrenNodes();
        assertNotNull(subSectionsNodes);
        assertEquals(2, subSectionsNodes.size());

        PublicationNode section22 = subSectionsNodes.get(1);
        assertEquals("section22", section22.getName());
        assertEquals(
                FilenameUtils.separatorsToSystem(rootFolder
                        + "/section2/section22"),
                FilenameUtils.separatorsToSystem(section22.getPath()));

        // check treeconfigName propagation
        assertEquals(tree.getConfigName(), tree.getTreeConfigName());
        assertEquals(tree.getConfigName(), section22.getTreeConfigName());
        assertEquals(tree.getSessionId(), section22.getSessionId());

        // test publish
        PublishedDocument pubDoc = tree.publish(doc2Publish, section22);
        assertNotNull(pubDoc);
        File container = new File(section22.getPath());
        File[] files = container.listFiles();
        assertEquals(1, files.length);

        assertEquals(doc2Publish.getRepositoryName(),
                pubDoc.getSourceRepositoryName());
        assertEquals(doc2Publish.getRef(), pubDoc.getSourceDocumentRef());

        List<PublishedDocument> foundDocs = tree.getExistingPublishedDocument(doc2publishLocation);
        assertNotNull(foundDocs);
        assertEquals(1, foundDocs.size());

        foundDocs = section22.getChildrenDocuments();
        assertNotNull(foundDocs);
        assertEquals(1, foundDocs.size());

        PublishedDocument pubDoc2 = tree.publish(doc2Publish, section2);
        foundDocs = tree.getExistingPublishedDocument(doc2publishLocation);
        assertNotNull(foundDocs);
        assertEquals(2, foundDocs.size());

        tree.unpublish(doc2Publish, section22);
        foundDocs = tree.getExistingPublishedDocument(doc2publishLocation);
        assertNotNull(foundDocs);
        assertEquals(1, foundDocs.size());

        tree.unpublish(doc2Publish, section2);
        foundDocs = tree.getExistingPublishedDocument(doc2publishLocation);
        assertNotNull(foundDocs);
        assertEquals(0, foundDocs.size());

        PublishedDocument pubDoc3 = tree.publish(doc2Publish, section2);
        foundDocs = tree.getExistingPublishedDocument(doc2publishLocation);
        assertNotNull(foundDocs);
        assertEquals(1, foundDocs.size());

        tree.unpublish(pubDoc3);
        foundDocs = tree.getExistingPublishedDocument(doc2publishLocation);
        assertNotNull(foundDocs);
        assertEquals(0, foundDocs.size());
    }

    private void registerFSTree(String treeName) throws Exception {
        PublisherService service = Framework.getLocalService(PublisherService.class);
        PublisherServiceImpl fullService = (PublisherServiceImpl) service;

        // dynamic contrib
        PublicationTreeConfigDescriptor desc = new PublicationTreeConfigDescriptor();
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put(AbstractBasePublicationTree.ROOT_PATH_KEY,
                rootFolder.getAbsolutePath());
        desc.setName(treeName);
        desc.setTree("LocalFSTree");
        desc.setFactory("LocalFile");
        desc.setParameters(parameters);

        fullService.registerContribution(desc,
                PublisherServiceImpl.TREE_CONFIG_EP, null);

        List<String> treeNames = service.getAvailablePublicationTree();
        assertTrue(treeNames.contains("TestingFSTree"));

        PublicationTree tree = service.getPublicationTree("TestingFSTree",
                session, null);
        assertNotNull(tree);
        assertEquals(treeName, tree.getConfigName());
        assertEquals(rootFolder.getName(), tree.getName());
        assertEquals("LocalFSPublicationTree", tree.getTreeType());
        assertEquals("FSPublicationNode", tree.getNodeType());
    }

    @Test
    public void testWithNonPublishedDocumentXmlFiles() throws Exception {
        createInitialDocs();
        createFSDirs();

        // add dummy file
        File dummyFile = new File(
                new Path(rootFolder.getAbsolutePath()).append("section1").toString(),
                "dummyFile");
        writeFile(dummyFile, "Dummy File");

        // add xml file
        File xmlFile = new File(new Path(rootFolder.getAbsolutePath()).append(
                "section2").toString(), "xmlFile");
        String xmlContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<html>" + "</html>";
        writeFile(xmlFile, xmlContent);

        registerFSTree("TestingFSTree");
        PublisherService service = Framework.getLocalService(PublisherService.class);
        PublicationTree tree = service.getPublicationTree("TestingFSTree",
                session, null);

        assertEquals(0,
                tree.getExistingPublishedDocument(doc2publishLocation).size());
    }

    private static void writeFile(File file, String content) throws Exception {
        BufferedWriter out = null;
        try {
            out = new BufferedWriter(new FileWriter(file));
            out.write(content);
        } finally {
            if (out != null) {
                out.flush();
                out.close();
            }
        }
    }

    @Test
    public void testFSIndex() throws Exception {
        createInitialDocs();
        createFSDirs();

        registerFSTree("TestingFSTree");

        PublisherService service = Framework.getLocalService(PublisherService.class);
        PublicationTree tree = service.getPublicationTree("TestingFSTree",
                session, null);

        List<PublicationNode> sectionsNodes = tree.getChildrenNodes();
        assertNotNull(sectionsNodes);
        assertEquals(3, sectionsNodes.size());

        // publish
        PublicationNode section1 = sectionsNodes.get(0);
        PublishedDocument pubDoc = tree.publish(doc2Publish, section1);
        assertNotNull(pubDoc);
        assertEquals(1,
                tree.getExistingPublishedDocument(doc2publishLocation).size());

        File indexFile = new File(rootFolder,
                LocalFSPublicationTree.INDEX_FILENAME);
        assertTrue(indexFile.exists());

        List<String> indexFileLines = FileUtils.readLines(indexFile);
        assertEquals(1, indexFileLines.size());

        PublicationNode section2 = sectionsNodes.get(1);
        PublishedDocument pubDoc2 = tree.publish(doc2Publish, section2);
        assertNotNull(pubDoc);
        assertEquals(2,
                tree.getExistingPublishedDocument(doc2publishLocation).size());

        assertTrue(indexFile.exists());

        indexFileLines = FileUtils.readLines(indexFile);
        assertEquals(2, indexFileLines.size());

        // unpublish
        tree.unpublish(pubDoc);
        assertEquals(1,
                tree.getExistingPublishedDocument(doc2publishLocation).size());
        indexFileLines = FileUtils.readLines(indexFile);
        assertEquals(1, indexFileLines.size());
        File indexFileTmp = new File(rootFolder,
                LocalFSPublicationTree.INDEX_FILENAME_TMP);
        assertFalse(indexFileTmp.exists());

        tree.unpublish(pubDoc2);
        assertEquals(0,
                tree.getExistingPublishedDocument(doc2publishLocation).size());
        indexFileLines = FileUtils.readLines(indexFile);
        assertEquals(0, indexFileLines.size());
    }

}
