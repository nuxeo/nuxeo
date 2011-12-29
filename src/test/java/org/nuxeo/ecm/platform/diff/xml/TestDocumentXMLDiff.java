/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     ataillefer
 */
package org.nuxeo.ecm.platform.diff.xml;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;
import org.nuxeo.ecm.platform.diff.model.DocumentDiff;
import org.nuxeo.ecm.platform.diff.model.PropertyDiff;
import org.nuxeo.ecm.platform.diff.model.SchemaDiff;
import org.nuxeo.ecm.platform.diff.service.DocumentDiffService;
import org.nuxeo.runtime.api.Framework;

/**
 * @author ataillefer
 */
public class TestDocumentXMLDiff extends SQLRepositoryTestCase {

    private static final Log LOGGER = LogFactory.getLog(TestDocumentXMLDiff.class);

    private static final String NUXEO_PLATFORM_DIFF_BUNDLE = "org.nuxeo.platform.diff";

    private DocumentDiffService docDiffService;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployBundle(NUXEO_PLATFORM_DIFF_BUNDLE);
        docDiffService = Framework.getService(DocumentDiffService.class);
        openSession();
    }

    @Override
    public void tearDown() throws Exception {
        closeSession();
        super.tearDown();
    }

    /**
     * Test doc diff.
     * 
     * @throws ClientException the client exception
     */
    @Test
    public void testDocDiff() throws ClientException {

        // Create two docs
        DocumentModel doc1 = createDoc1();
        DocumentModel doc2 = createDoc2();
        session.save();

        // Do doc diff
        DocumentDiff docDiff = docDiffService.diff(session, doc1, doc2);
        assertEquals("Wrong schema count.", 1, docDiff.getSchemaCount());

        SchemaDiff schemaDiff = docDiff.getSchemaDiff("dublincore");
        assertNotNull("Schema diff should not be null", schemaDiff);
        assertEquals("Wrong field count.", 1, schemaDiff.getFieldCount());

        PropertyDiff titleDiff = schemaDiff.getFieldDiff("title");
        assertNotNull("Field diff should not be null", titleDiff);
        assertEquals("Wrong left value.", "My first note",
                titleDiff.getLeftValue());
        assertEquals("Wrong right value.", "My second note",
                titleDiff.getRightValue());

    }

    /**
     * Creates the doc1.
     * 
     * @return the document model
     * @throws ClientException the client exception
     * @throws PropertyException the property exception
     */
    protected DocumentModel createDoc1() throws ClientException,
            PropertyException {

        DocumentModel doc1 = session.createDocumentModel("/", "doc1", "Note");
        doc1.setPropertyValue("dc:title", "My first note");
        doc1 = session.createDocument(doc1);
        return doc1;
    }

    /**
     * Creates the doc2.
     * 
     * @return the document model
     * @throws ClientException the client exception
     * @throws PropertyException the property exception
     */
    protected DocumentModel createDoc2() throws ClientException,
            PropertyException {

        DocumentModel doc2 = session.createDocumentModel("/", "doc2", "Note");
        doc2.setPropertyValue("dc:title", "My second note");
        doc2 = session.createDocument(doc2);
        return doc2;
    }

}
