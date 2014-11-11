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
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.api.impl.blob.ByteArrayBlob;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.model.Session;
import org.nuxeo.ecm.core.query.Query;
import org.nuxeo.ecm.core.query.QueryException;
import org.nuxeo.ecm.core.query.QueryResult;
import org.nuxeo.ecm.core.repository.jcr.JCRDocument;
import org.nuxeo.ecm.core.repository.jcr.properties.BlobProperty;
import org.nuxeo.ecm.core.repository.jcr.testing.RepositoryTestCase;

/**
 * Test Queries.
 *
 * @author DM
 */
public class TestFtsQuery extends RepositoryTestCase {

    private static final Log log = LogFactory.getLog(TestFtsQuery.class);

    private static final String docPath = "SampleDoc.doc";

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

        //if (session != null)
        //    session.close();
        session = null;
        root = null;

        super.tearDown();
    }

    public void testQuery() throws Exception {
        Query qry = session.createQuery("SELECT * FROM document", Query.Type.NXQL);
        QueryResult qr = qry.execute();
        while (qr.next()) {
            final String str = qr.getString("jcr:primaryType");
            assertTrue(str.equals("ecmdt:Root") || str.equals("ecmdt:Folder")
                    || str.equals("ecmdt:File"));
        }
    }

    private void createDocs() throws Exception {
        // put some data in workspace
        Document folder1 = root.addChild("testfolder1", "Folder");

        // create Doc file 1
        Document file1 = folder1.addChild("testfile1", "File");
        file1.setString("title", "testfile1_Title");
        file1.setString("description", "testfile1_description");

        URL url = Thread.currentThread().getContextClassLoader().getResource(docPath);
        File file = new File(url.toURI());

        final byte[] docContent = FileUtils.readBytes(file);

        Blob fileContent = new ByteArrayBlob(docContent,
                "application/msword");

        file1.setPropertyValue("content", fileContent);
        file1.setPropertyValue("filename", file.getName());

        // create doc 2
        Document file2 = folder1.addChild("testfile2", "File");
        file2.setString("title", "testfile2_Title");
        file2.setString("description", "testfile2_DESCRIPTION2");

        Document file3 = folder1.addChild("testfile3", "File");
        file3.setString("title", "testfile3_Title");
        file3.setString("description", "testfile3_desc1 testfile3_desc2,  testfile3_desc3");

        session.save();

//        try {
//            Thread.currentThread().sleep(5000);
//        } catch (InterruptedException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }
    }

    private void removeDocs() throws DocumentException {
        root.getChild("testfolder1").remove();

        session.save();
//        try {
//            Thread.currentThread().sleep(5000);
//        } catch (InterruptedException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }
    }

    // Doesn't work. TODO: make it pass of remove if irrelevant
    public void __testXPathContain() throws Exception {
        final String logPrefix = "<testJCRXPathContain> ";

        log.info(logPrefix + "...");

        //createDocs();

        //Query qry = session.createQuery("SELECT * FROM document WHERE filename LIKE '%'");
        //QueryResult qr = qry.execute();

        // Query qry = session.createQuery("SELECT * FROM ecmnt:document");
        //Query qry = session.createQuery("SELECT * FROM ecmnt:document WHERE CONTAINS (., 'Nuxeo')");
        //QueryResult qr = qry.executeFTS(true);

        //File f = new File("jcrdump.xml");
        //System.out.println(f);
        //FileOutputStream fos = new FileOutputStream(f);
        //((JCRSession)session).jcrSession().exportSystemView("/", fos, true, false);

        //final String xpathQ = "//*"; // gets all
        //final String xpathQ = "element(*)"; // gets root node
        //final String xpathQ = "//element(*, ecmst:file)"; // gets the three nodes
        //final String xpathQ = "//element(*)[jcr:contains(.,'SampleDoc*')]"; // gets the node with file name...
        final String xpathQ = "//element(*)[jcr:contains(.,'*Nuxeo*')]"; // gets the node with file name...

        //final String xpathQ = "element(*, ecmnt:document)";
        //final String xpathQ = "element(*, ecmnt:document)[jcr:contains(.,'Nuxeo')] order by jcr:score() descending";
        Query qry = session.createQuery(xpathQ, Query.Type.XPATH);
        QueryResult qr = qry.execute();

        printResults(qr, logPrefix);

        assertEquals(1, qr.count());
    }

    /**
     * Tests selection with full text by node extracted binary content.
     *
     * @throws QueryException
     * @throws RepositoryException
     * @throws DocumentException
     * @throws IOException
     */
    public void testXPathWithDocTypeExtractedContent_file1() throws
            QueryException, RepositoryException, DocumentException,
            IOException {
        final String logPrefix = "<testJCRXPathContainProp_file1> ";

        log.info(logPrefix + "...");

        //createDocs();

        // will select the first file by document name
        final String xpathQ = "//element(*, ecmnt:document)[jcr:contains(.,'*testfile1*')]";
        final Query qry = session.createQuery(xpathQ, Query.Type.XPATH);
        QueryResult qr = qry.execute();
        printResults(qr, logPrefix);
        assertEquals(1, qr.count());

        // check we have the good object
        qr = qry.execute();
        assertTrue(qr.next());
        JCRDocument doc = (JCRDocument) qr.getObject();
        assertEquals("testfile1_Title", doc.getString("title"));
        assertEquals("testfile1_description", doc.getString("description"));
    }


    public void testXPathContainAndJoin() throws Exception {
        final String logPrefix = "<testJCRXPathContainAndJoin> ";

        log.info(logPrefix + "...");

        //createDocs();

        final String xpathQ = "//element(*, ecmnt:document)[jcr:contains(.,'*testfile1*')]";

        // + "order by jcr:score() descending"

        Query qry = session.createQuery(xpathQ, Query.Type.XPATH);
        QueryResult qr = qry.execute();

        printResults(qr, logPrefix);

        assertEquals(1, qr.count());
    }


    /**
     * Tests selection with full text by node properties values.
     *
     * @throws QueryException
     * @throws RepositoryException
     * @throws DocumentException
     * @throws IOException
     */
    public void testXPathContainProp_file1() throws QueryException,
            RepositoryException, DocumentException, IOException {
        final String logPrefix = "<testJCRXPathContainProp_file1> ";

        log.info(logPrefix + "...");

        //createDocs();

        // will select the first file by document name
        final String xpathQ = "//element(*)[jcr:contains(.,'*testfile1*')]";
        final Query qry = session.createQuery(xpathQ, Query.Type.XPATH);
        QueryResult qr = qry.execute();
        printResults(qr, logPrefix);
        assertEquals(1, qr.count());

        // check we have the good object
        qr = qry.execute();
        assertTrue(qr.next());
        JCRDocument doc = (JCRDocument) qr.getObject();
        assertEquals("testfile1_Title", doc.getString("title"));
        assertEquals("testfile1_description", doc.getString("description"));
    }

    /**
     * Test selection with full text by node properties values.
     *
     * @throws QueryException
     * @throws RepositoryException
     * @throws DocumentException
     * @throws IOException
     */
    public void testXPathWithDocTypeContainProp_file1() throws QueryException,
            RepositoryException, DocumentException, IOException {
        final String logPrefix = "<testJCRXPathContainProp_file1> ";

        log.info(logPrefix + "...");

        //createDocs();

        // will select the first file by document name
        final String xpathQ = "//element(*, ecmnt:document)[jcr:contains(.,'*testfile1*')]";
        final Query qry = session.createQuery(xpathQ, Query.Type.XPATH);
        QueryResult qr = qry.execute();
        printResults(qr, logPrefix);
        assertEquals(1, qr.count());

        // check we have the good object
        qr = qry.execute();
        assertTrue(qr.next());
        JCRDocument doc = (JCRDocument) qr.getObject();
        assertEquals("testfile1_Title", doc.getString("title"));
        assertEquals("testfile1_description", doc.getString("description"));
    }

    /**
     * Test selection with full text by node properties values.
     *
     * @throws QueryException
     *
     * @throws QueryException
     * @throws RepositoryException
     * @throws RepositoryException
     * @throws DocumentException
     * @throws DocumentException
     * @throws IOException
     */
    public void testXPathContainProp_file2() throws QueryException, RepositoryException, DocumentException, IOException {
        final String logPrefix = "<testJCRXPathContainProp_file2> ";

        log.info(logPrefix + "...");

        //createDocs();

        // will select the second file by description
        final String xpathQ = "//element(*)[jcr:contains(.,'*testfile2_DESCRIPTION2*')]";
        final Query qry = session.createQuery(xpathQ, Query.Type.XPATH);
        QueryResult qr = qry.execute();
        printResults(qr, logPrefix);
        assertEquals(1, qr.count());

        // check we have the good object
        qr = qry.execute();
        assertTrue(qr.next());
        JCRDocument doc = (JCRDocument) qr.getObject();
        assertEquals("testfile2_Title", doc.getString("title"));
        assertEquals("testfile2_DESCRIPTION2", doc.getString("description"));
    }

    public void OBSOLETEtestXPathInPath() throws Exception {
        final String logPrefix = "<testXPathStartingWith> ";

        log.info(logPrefix + "...");

        // will select the second file by description
        // real jcr path : //ecm:root/element(*)
        // real jcr path : //ecm:root/ecm:children/element(*)
        // real jcr path : //ecm:root/ecm:children/testfolder1/element(*)
//      real jcr path : //ecm:root/ecm:children/testfolder1//element(*, ecmnt:document)
        final String xpathQ = "/testfolder1/element(*)[jcr:contains(.,'*testfile2_DESCRIPTION2*')]";
        final Query qry = session.createQuery(xpathQ, Query.Type.XPATH);
        QueryResult qr = qry.execute();
        printResults(qr, logPrefix);
        assertEquals(1, qr.count());
    }

    public void __testXPathInPathAgainstExtractedText() throws Exception {
        final String logPrefix = "<testXPathStartingWith> ";

        log.info(logPrefix + "...");

        // will select the second file by description
        // real jcr path : //ecm:root/element(*)
        // real jcr path : //ecm:root/ecm:children/element(*)
        // real jcr path : //ecm:root/ecm:children/testfolder1/element(*)
//      real jcr path : //ecm:root/ecm:children/testfolder1//element(*, ecmnt:document)
        final String xpathQ = "/testfolder1/element(*)[jcr:contains(.,'*nuxeo*')]";
        final Query qry = session.createQuery(xpathQ, Query.Type.XPATH);
        QueryResult qr = qry.execute();
        printResults(qr, logPrefix);
        assertEquals(1, qr.count());

    }

    public void OBSOLETEtestXPathQueryAfterEdit() throws Exception {
        final String logPrefix = "<testXPathQueryAfterEdit> ";

        log.info(logPrefix + "...");

        final String xpathQ = "/testfolder1/element(*)[jcr:contains(.,'*testfile1_description*')]";
        final Query qry = session.createQuery(xpathQ, Query.Type.XPATH);
        QueryResult qr = qry.execute();
        printResults(qr, logPrefix);
        assertEquals(1, qr.count());

        final Document doc = (JCRDocument) qr.getObject();
        // need to do the test for document with attached content
        final BlobProperty blob = (BlobProperty) doc.getProperty("content");
        assertNotNull(blob);
        //System.err.println(blob.getMimeType());

        // edit the document
        doc.setPropertyValue("description", "testfile1_description_CHANGED");
        doc.save();

        session.save();

        final String xpathQ2 = "/testfolder1/element(*)[jcr:contains(.,'*testfile1_description_CHANGED*')]";
        final Query qry2 = session.createQuery(xpathQ2, Query.Type.XPATH);
        QueryResult qr2 = qry.execute();
        printResults(qr2, logPrefix);
        assertEquals(1, qr2.count());
    }


    /* -- these won't work any more :
     * TODO enable them when CONTAINS is added to NXQL
    public void testSQLContain() throws Exception {
        final String logPrefix = "<testJCRSQLContain> ";

        log.info(logPrefix + "...");

        //createDocs();

        final String sql = "SELECT * FROM nt:resource WHERE CONTAINS (*, '*Nuxeo*')";

        Query qry = session.createQuery(sql);
        QueryResult qr = qry.executeFTS(true);

        printResults(qr, logPrefix);

        // -- NOT TRUE --
        // won't expect any result because the indexed word is linked to the master
        // of the nt:resource which is of type ecmnt:document
        // --------------
        assertEquals(1, qr.count());
    }

    public void testSQLContainForMasterNodeFields() throws Exception {
        final String logPrefix = "<testJCRSQLContainForMasterNodeFields> ";

        log.info(logPrefix + "...");

        //createDocs();

        final String sql = "SELECT * FROM ecmnt:document WHERE CONTAINS (*, 'Sample*')";

        Query qry = session.createQuery(sql);
        QueryResult qr = qry.executeFTS(true);

        printResults(qr, logPrefix);

        assertEquals(1, qr.count());
    }

    public void testSQLContainAndJoin() throws Exception {
        final String logPrefix = "<testJCRSQLContainAndJoin> ";

        log.info(logPrefix + "...");

        //createDocs();

        String sql = "SELECT * FROM ecmnt:document WHERE CONTAINS (*, 'Sample*')";
        //sql += " OUTER JOIN ";
       // sql += " (SELECT * FROM nt:resource WHERE CONTAINS (*, '*Nuxeo*'))";
        // on....

        Query qry = session.createQuery(sql);
        QueryResult qr = qry.executeFTS(true);

        printResults(qr, logPrefix);

        assertEquals(1, qr.count());
    }
    */

    /**
     * Convenient method to display a query result. It prints several node
     * (of the JCRDocument) properties.
     *
     * @param qr the QueryResult object
     * @param logPrefix
     * @throws RepositoryException
     * @throws QueryException
     */
    private void printResults(final QueryResult qr, final String logPrefix)
            throws RepositoryException, QueryException {
        while (qr.next()) {

            final JCRDocument doc = (JCRDocument) qr.getObject();
            final String str = getNodeDesc(doc.getNode(), "\n");
            log.info(logPrefix + '\n' + str + '\n');
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
