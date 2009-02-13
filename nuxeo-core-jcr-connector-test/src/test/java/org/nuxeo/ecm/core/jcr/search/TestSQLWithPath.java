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

package org.nuxeo.ecm.core.jcr.search;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.ByteArrayBlob;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.model.Session;
import org.nuxeo.ecm.core.query.Query;
import org.nuxeo.ecm.core.query.QueryException;
import org.nuxeo.ecm.core.query.QueryResult;
import org.nuxeo.ecm.core.repository.jcr.JCRDocument;
import org.nuxeo.ecm.core.repository.jcr.testing.RepositoryTestCase;

/**
 * Test Queries.
 *
 * @author DM
 */
public class TestSQLWithPath extends RepositoryTestCase {

    private static final Log log = LogFactory.getLog(TestSQLWithPath.class);

    private static final String DOC_PATH = "SampleDoc.doc";

    private Session session;

    private Document root;

    @Override
    public void setUp() throws Exception {
        log.info("Initializing NX Core for local tests");
        super.setUp();
        session = getRepository().getSession(null);
        root = session.getRootDocument();
        createDocs();
    }

    @Override
    public void tearDown() throws Exception {
        log.info("Shutting down NX Core for local tests");
        removeDocs();

        // if (session != null)
        // session.close();
        session = null;
        root = null;

        super.tearDown();
    }

    /**
     * Creates the following structure of documents:
     *
     * <pre>
     *  -- root
     *     +- testfolder1
     *        -- testfile1
     *        -- testfile2
     *        -- testfile3
     *    +- tesfolder2
     *        +- testfolder3
     *            -- testfile4
     * </pre>
     *
     */
    private void createDocs() throws Exception {
        // put some data in workspace
        Document folder1 = root.addChild("testfolder1", "Folder");

        // create Doc file 1
        Document file1 = folder1.addChild("testfile1", "File");
        file1.setString("dc:title", "testfile1_Title");
        file1.setString("dc:description", "testfile1_description");

        // adding an attachment
        attacheFileTo(file1);

        // create doc 2
        Document file2 = folder1.addChild("testfile2", "File");
        file2.setString("dc:title", "testfile2_Title");
        file2.setString("dc:description", "testfile2_DESCRIPTION2");

        Document file3 = folder1.addChild("testfile3", "File");
        file3.setString("dc:title", "testfile3_Title");
        file3.setString("dc:description",
                "testfile3_desc1 testfile3_desc2,  testfile3_desc3");

        // create folder 2
        Document folder2 = root.addChild("testfolder2", "Folder");

        // create folder 3
        Document folder3 = folder2.addChild("testfolder3", "Folder");

        // create file 4
        Document file4 = folder3.addChild("testfile4", "File");
        file4.setString("dc:title", "testfile4_Title");
        file4.setString("dc:description", "testfile4_DESCRIPTION4");

        session.save();
    }

    private static void attacheFileTo(Document doc) throws URISyntaxException,
            IOException, DocumentException {
        URL url = Thread.currentThread().getContextClassLoader().getResource(
                DOC_PATH);
        File file = new File(url.toURI());

        final byte[] docContent = FileUtils.readBytes(file);
        ByteArrayBlob fileContent = new ByteArrayBlob(docContent,
                "application/msword");
        doc.setPropertyValue("content", fileContent);
        doc.setPropertyValue("filename", file.getName());
    }

    private void removeDocs() throws DocumentException {
        root.getChild("testfolder1").remove();
        root.getChild("testfolder2").remove();

        session.save();
    }

    public void testSQLWithLike() throws Exception {
        final String logPrefix = "<testJCRXPathContain> ";
        log.info(logPrefix + "...");

        final String sql = "SELECT * FROM document WHERE dc:description LIKE 'testfile%' ORDER BY filename";

        Query qry = session.createQuery(sql, Query.Type.NXQL);
        QueryResult qr = qry.execute();

        printResults(qr, logPrefix);

        assertEquals(4, qr.count());
    }

    public void testSQLSubpathWithLike() throws Exception {
        final String logPrefix = "<testJCRXPathContain> ";
        log.info(logPrefix + "...");

        final String sql = "SELECT * FROM document WHERE ecm:path STARTSWITH '/'";

        Query qry = session.createQuery(sql, Query.Type.NXQL);
        QueryResult qr = qry.execute();

        printResults(qr, logPrefix);

        assertEquals(7, qr.count());
    }

    public void OBSOLETEtestSQLSubpathWithLike1() throws Exception {
        final String logPrefix = "<testJCRXPathContain> ";
        log.info(logPrefix + "...");

        final String sql = "SELECT * FROM document WHERE ecm:path STARTSWITH \"/testfolder1/\"";

        Query qry = session.createQuery(sql, Query.Type.NXQL);
        QueryResult qr = qry.execute();

        printResults(qr, logPrefix);

        assertEquals(3, qr.count());
    }

    public void OBSOLETEtestSQLSubpathWithLike2() throws Exception {
        final String logPrefix = "<testJCRXPathContain> ";
        log.info(logPrefix + "...");

        final String sql = "SELECT * FROM document WHERE ecm:path STARTSWITH \"/testfolder2/\"";

        Query qry = session.createQuery(sql, Query.Type.NXQL);
        QueryResult qr = qry.execute();

        printResults(qr, logPrefix);

        assertEquals(2, qr.count());
    }

    public void testSQLSubpathWithLike3() throws Exception {
        final String logPrefix = "<testJCRXPathContain> ";
        log.info(logPrefix + "...");

        final String sql = "SELECT * FROM document WHERE dc:title='testfile1_Title' AND ecm:path STARTSWITH \"/\"";

        Query qry = session.createQuery(sql, Query.Type.NXQL);
        QueryResult qr = qry.execute();

        printResults(qr, logPrefix);

        assertEquals(1, qr.count());
    }

    public void testSQLSubpathWithLike4() throws Exception {
        final String logPrefix = "<testJCRXPathContain> ";
        log.info(logPrefix + "...");

        final String sql = "SELECT * FROM document WHERE dc:title LIKE 'testfile%' AND ecm:path STARTSWITH \"/\"";

        Query qry = session.createQuery(sql, Query.Type.NXQL);
        QueryResult qr = qry.execute();

        printResults(qr, logPrefix);

        assertEquals(4, qr.count());
    }

    public void OBSOLETEtestSQLSubpathWithLike4x2() throws Exception {
        final String logPrefix = "<testJCRXPathContain> ";
        log.info(logPrefix + "...");

        final String sql = "SELECT * FROM document WHERE dc:title LIKE 'testfile%' AND ecm:path STARTSWITH \"/testfolder2\"";

        Query qry = session.createQuery(sql, Query.Type.NXQL);
        QueryResult qr = qry.execute();

        printResults(qr, logPrefix);

        assertEquals(1, qr.count());
    }

    public void OBSOLETEtestSQLSubpathWithLike4x3() throws Exception {
        final String logPrefix = "<testJCRXPathContain> ";
        log.info(logPrefix + "...");

        final String sql = "SELECT * FROM document WHERE dc:title LIKE 'testfile%' AND ecm:path STARTSWITH \"/testfolder1\"";

        Query qry = session.createQuery(sql, Query.Type.NXQL);
        QueryResult qr = qry.execute();

        printResults(qr, logPrefix);

        assertEquals(3, qr.count());
    }

    public void testSQLReindexEditedDocument() throws Exception {

        String sql = "SELECT * FROM document WHERE dc:title LIKE 'testfile1_Ti%'";
        Query qry = session.createQuery(sql, Query.Type.NXQL);
        QueryResult qr = qry.execute();
        assertEquals(1, qr.count());

        Document doc = root.getChild("testfolder1").getChild("testfile1");

        // edit file1
        doc.setString("dc:description", "testfile1_description");
        doc.setPropertyValue("content", null);
        session.save();

        // rerunning the same query
        sql = "SELECT * FROM document WHERE dc:title LIKE 'testfile1_Ti%'";
        qry = session.createQuery(sql, Query.Type.NXQL);
        qr = qry.execute();
        assertEquals(1, qr.count());

        // editing the title
        doc.setString("dc:title", "testfile1_ModifiedTitle");
        attacheFileTo(doc);
        session.save();

        // rerunning the same query
        sql = "SELECT * FROM document WHERE dc:title LIKE 'testfile1_Ti%'";
        qry = session.createQuery(sql, Query.Type.NXQL);
        qr = qry.execute();
        assertEquals(0, qr.count());

        // editing the title
        doc.setString("dc:description", "Yet another description");
        session.save();


        // adjusting the query to the new title
        sql = "SELECT * FROM document WHERE dc:title LIKE 'testfile1_Mo%'";
        qry = session.createQuery(sql, Query.Type.NXQL);
        qr = qry.execute();
        assertEquals(1, qr.count());

    }

    public void testSQLFromClause() throws Exception {

        String sql = "SELECT * FROM File";

        Query qry = session.createQuery(sql, Query.Type.NXQL);
        QueryResult qr = qry.execute();
        assertEquals(4, qr.count());
        for (DocumentModel dm : qr.getDocumentModels()) {
            assertEquals("File", dm.getType());
        }

        sql = "SELECT * FROM Folder";
        qry = session.createQuery(sql, Query.Type.NXQL);
        qr = qry.execute();
        assertEquals(4, qr.count());
        for (DocumentModel dm : qr.getDocumentModels()) {
            assertEquals("Folder", dm.getType());
        }

        // XXX: The following does not work since compound FROM clause
        // are meant to build joins for JCR-SQL. We need devise a proper
        // semantics for NXQL
        //
        // sql = "SELECT * FROM Folder, File";
        // qry = session.createQuery(sql, Query.Type.NXQL);
        // qr = qry.execute();
        // assertEquals(8, qr.count());
        // String[] validTypes = {"Folder", "File"};
        // List<String> typeList = Arrays.asList(validTypes);
        // for (DocumentModel dm: qr.getDocumentModels()) {
        // assertTrue(typeList.contains(dm.getType()));
        // }

    }

    public void __testSQLFulltextAndSubpath() throws Exception {
        final String logPrefix = "<testJCRXPathContain> ";
        log.info(logPrefix + "...");

        final String sql = "SELECT * FROM document WHERE content LIKE '% Nuxeo%' AND ecm:path STARTSWITH \"/\"";

        Query qry = session.createQuery(sql, Query.Type.NXQL);
        QueryResult qr = qry.execute();

        printResults(qr, logPrefix);

        assertEquals(1, qr.count());
    }

    /**
     * Convenience method to display a query result. It prints several node (of
     * the JCRDocument) properties.
     *
     * @param qr the QueryResult object
     * @param logPrefix
     * @throws RepositoryException
     * @throws QueryException
     */
    private static void printResults(final QueryResult qr, final String logPrefix)
            throws RepositoryException, QueryException {
        while (qr.next()) {

            final Object row = qr.getObject();

            if (row instanceof JCRDocument) {
                final JCRDocument doc = (JCRDocument) row;
                final String str = getNodeDesc(doc.getNode(), "\n");
                log.info(logPrefix + '\n' + str + '\n');
            } else {
                log.info(logPrefix + '\n' + row + '\n');
            }
        }
    }

    /**
     * Creates a node string description usable in debugging.
     *
     * @param node
     * @param sep a node props separator
     * @return
     * @throws RepositoryException
     */
    @SuppressWarnings({"StringConcatenationInsideStringBufferAppend"})
    private static String getNodeDesc(Node node, final String sep)
            throws RepositoryException {
        PropertyIterator pi = node.getProperties();

        final StringBuilder buf = new StringBuilder();

        buf.append("node path : " + node.getPath());

        // list some properties
        while (pi.hasNext()) {
            Property p = (Property) pi.next();
            if (p.getType() == PropertyType.BINARY) {
                buf.append(sep + p.getName() + " = " + p.getStream());
            }

            if (p.getType() == PropertyType.STRING) {
                String vals;
                try {
                    vals = p.getString();
                } catch (ValueFormatException e) {
                    vals = Arrays.toString(p.getValues());
                }
                buf.append(sep + p.getName() + " = " + vals);
            }
        }

        return buf.toString();
    }

}
