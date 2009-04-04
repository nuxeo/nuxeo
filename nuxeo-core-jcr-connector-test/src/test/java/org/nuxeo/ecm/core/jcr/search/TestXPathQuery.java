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
public class TestXPathQuery extends RepositoryTestCase {

    private static final Log log = LogFactory.getLog(TestXPathQuery.class);

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

    /**
     * Creates the following structure of documents:
     * <pre>
     * -- root
     *    +- testfolder1
     *       -- testfile1
     *       -- testfile2
     *       -- testfile3
     *   +- tesfolder2
     *       +- testfolder3
     *           -- testfile4
     * </pre>
     * @throws Exception
     */
    private void createDocs() throws Exception {
        // put some data in workspace
        Document folder1 = root.addChild("testfolder1", "Folder");

        // create Doc file 1
        Document file1 = folder1.addChild("testfile1", "File");
        file1.setString("dc:title", "testfile1_Title");
        file1.setString("dc:description", "testfile1_description");

        URL url = Thread.currentThread().getContextClassLoader().getResource(docPath);
        File file = new File(url.toURI());

        final byte[] docContent = FileUtils.readBytes(file);

        ByteArrayBlob fileContent = new ByteArrayBlob(docContent,
                "application/msword");

        file1.setPropertyValue("content", fileContent);
        file1.setPropertyValue("filename", file.getName());

        // create doc 2
        Document file2 = folder1.addChild("testfile2", "File");
        file2.setString("dc:title", "testfile2_Title");
        file2.setString("dc:description", "testfile2_DESCRIPTION2");

        Document file3 = folder1.addChild("testfile3", "File");
        file3.setString("dc:title", "testfile3_Title");
        file3.setString("dc:description", "testfile3_desc1 testfile3_desc2,  testfile3_desc3");

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

    private void removeDocs() throws DocumentException {
        root.getChild("testfolder1").remove();
        root.getChild("testfolder2").remove();

        session.save();
    }

    public void testXPathWithLike() throws Exception {
        final String logPrefix = "<testJCRXPathContain> ";
        log.info(logPrefix + "...");

        final String xpathQ = "//element(*, ecmnt:document)[jcr:like(@dc:description, 'testfile%')] order by @filename descending";

        Query qry = session.createQuery(xpathQ, Query.Type.XPATH);
        QueryResult qr = qry.execute();

        printResults(qr, logPrefix);

        assertEquals(4, qr.count());
    }

    public void OBSOLETEtestXPathSubpathWithLike() throws Exception {
        final String logPrefix = "<testJCRXPathContain> ";
        log.info(logPrefix + "...");

        final String xpathQ = "//testfolder1/element(*, ecmnt:document)[jcr:like(@dc:description, 'testfile%')] "
            + "order by @filename descending";

        Query qry = session.createQuery(xpathQ, Query.Type.XPATH);
        QueryResult qr = qry.execute();

        printResults(qr, logPrefix);

        assertEquals(3, qr.count());
    }

    public void OBSOLETEtestXPathSubpathWithLike2() throws Exception {
        final String logPrefix = "<testJCRXPathContain> ";
        log.info(logPrefix + "...");

        final String xpathQ = "//testfolder2/testfolder3/element(*, ecmnt:document)[jcr:like(@dc:description, 'testfile%')] "
            + "order by @filename descending";

        Query qry = session.createQuery(xpathQ, Query.Type.XPATH);
        QueryResult qr = qry.execute();

        printResults(qr, logPrefix);

        assertEquals(1, qr.count());
    }

    public void testXPathSubpathWithLike3() throws Exception {
        final String logPrefix = "<testJCRXPathContain> ";
        log.info(logPrefix + "...");

        final String xpathQ = "//testfolder1/notexistent/element(*, ecmnt:document)[jcr:like(@dc:description, 'testfile%')] "
            + "order by @filename descending";

        Query qry = session.createQuery(xpathQ, Query.Type.XPATH);
        QueryResult qr = qry.execute();

        printResults(qr, logPrefix);

        assertEquals(0, qr.count());
    }

    /**
     * Convenient method to display a query result. It prints several node
     * (of the JCRDocument) properties
     *
     * @param qr the QueryResult object
     * @param logPrefix
     * @throws RepositoryException
     * @throws QueryException
     */
    private static void printResults(final QueryResult qr, final String logPrefix)
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
