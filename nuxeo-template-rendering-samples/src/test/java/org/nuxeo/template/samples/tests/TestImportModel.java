/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id: TestExportedDocument.java 29029 2008-01-14 18:38:14Z ldoguin $
 */

package org.nuxeo.template.samples.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;
import org.nuxeo.template.api.adapters.TemplateBasedDocument;
import org.nuxeo.template.samples.ModelImporter;

public class TestImportModel extends SQLRepositoryTestCase {

    DocumentModel rootDocument;

    DocumentModel workspace;

    DocumentModel docToExport;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        deployBundle("org.nuxeo.ecm.core.api");
        deployBundle("org.nuxeo.template.manager.api");
        deployBundle("org.nuxeo.template.manager");

        openSession();
    }

    @After
    public void tearDown() throws Exception {
        closeSession();
        super.tearDown();
    }

    @Test
    public void testImportContentTemplateArchive() throws Exception {

        ModelImporter importer = new ModelImporter(session);

        importer.importModels();

        session.save();
        // check result

        StringBuffer sb = new StringBuffer();
        DocumentModelList docs = session.query("select * from Document order by ecm:path");
        for (DocumentModel doc : docs) {
            sb.append("path: " + doc.getPathAsString() + " type: "
                    + doc.getType() + " title:" + doc.getTitle() + " name:"
                    + doc.getName() + " uuid:" + doc.getId());
            TemplateBasedDocument templateDoc = doc.getAdapter(TemplateBasedDocument.class);
            if (templateDoc != null) {
                for (String tName : templateDoc.getTemplateNames()) {
                    sb.append(" target: " + tName + "-"
                            + templateDoc.getSourceTemplateDocRef(tName));
                    assertTrue(session.exists(templateDoc.getSourceTemplateDocRef(tName)));
                }
            }
            sb.append("\n");
        }

        assertEquals(2, docs.size());

        String dump = sb.toString();
        System.out.println("Import completed : " + docs.size() + " docs");
        System.out.println(dump);

    }
}
