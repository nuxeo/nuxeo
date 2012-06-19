/*
 * (C) Copyright 2006-20012 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 */

package org.nuxeo.template.samples.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolder;
import org.nuxeo.ecm.core.convert.api.ConversionService;
import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.template.api.adapters.TemplateBasedDocument;
import org.nuxeo.template.processors.xdocreport.ZipXmlHelper;

public class TestImportedModelRendering extends SQLRepositoryTestCase {

    DocumentModel rootDocument;

    DocumentModel workspace;

    DocumentModel docToExport;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        deployBundle("org.nuxeo.ecm.platform.content.template");
        deployBundle("org.nuxeo.ecm.core.event");
        deployBundle("org.nuxeo.ecm.core.convert.api");
        deployBundle("org.nuxeo.ecm.platform.mimetype.api");
        deployBundle("org.nuxeo.ecm.platform.mimetype.core");
        deployBundle("org.nuxeo.ecm.core.convert");
        deployBundle("org.nuxeo.ecm.core.convert.plugins");
        deployBundle("org.nuxeo.ecm.platform.convert");
        deployBundle("org.nuxeo.ecm.platform.preview");
        deployBundle("org.nuxeo.ecm.platform.dublincore");

        deployBundle("org.nuxeo.template.manager.api");
        deployBundle("org.nuxeo.template.manager");
        deployBundle("org.nuxeo.template.manager.jaxrs");
        deployBundle("org.nuxeo.template.manager.xdocreport");
        deployBundle("org.nuxeo.template.manager.jxls");
        deployBundle("org.nuxeo.template.manager.samples");

        fireFrameworkStarted();

        openSession();
    }

    @After
    public void tearDown() throws Exception {
        closeSession();
        super.tearDown();
    }

    @Test
    public void testNote4Web() throws Exception {

        PathRef ref = new PathRef("default-domain/workspaces/templateSamples/");
        DocumentModel sampleFolder = session.getDocument(ref);
        assertNotNull(sampleFolder);

        ref = new PathRef("default-domain/workspaces/templateSamples/webnote");
        DocumentModel note4Web = session.getDocument(ref);

        TemplateBasedDocument note4WebTemplate = note4Web.getAdapter(TemplateBasedDocument.class);
        assertNotNull(note4WebTemplate);

        List<String> templateNames = note4WebTemplate.getTemplateNames();
        assertEquals(1, templateNames.size());
        assertEquals("WebTemplate4Note", templateNames.get(0));

        Blob blob = note4WebTemplate.renderWithTemplate("WebTemplate4Note");
        assertNotNull(blob);
        String htmlContent = blob.getString();
        assertTrue(htmlContent.contains("<link class=\"component\" href=\"/nuxeo/site/templates/doc/"
                + note4Web.getId() + "/"));
        assertTrue(htmlContent.contains("<title> Note4Web </title>"));
        assertTrue(htmlContent.contains("<img src=\"/nuxeo/nxbigfile/test/"
                + note4Web.getId() + "/blobholder:1/"));
    }

    @Test
    public void testSampleNote() throws Exception {

        PathRef ref = new PathRef("default-domain/workspaces/templateSamples/");
        DocumentModel sampleFolder = session.getDocument(ref);
        assertNotNull(sampleFolder);

        ref = new PathRef("default-domain/workspaces/templateSamples/note");
        DocumentModel note = session.getDocument(ref);

        TemplateBasedDocument noteTemplate = note.getAdapter(TemplateBasedDocument.class);
        assertNotNull(noteTemplate);

        List<String> templateNames = noteTemplate.getTemplateNames();
        assertEquals(1, templateNames.size());
        assertEquals("Note Wrapper", templateNames.get(0));

        Blob blob = noteTemplate.renderWithTemplate("Note Wrapper");
        assertNotNull(blob);
        assertTrue(blob.getFilename().endsWith(".pdf"));

        ConversionService cs = Framework.getLocalService(ConversionService.class);

        BlobHolder textBH = cs.convertToMimeType("text/plain",
                new SimpleBlobHolder(blob), new HashMap<String, Serializable>());
        assertNotNull(textBH);
        String text = textBH.getBlob().getString();

        // check TOC
        assertTrue(text.contains("1   Overview"));
        assertTrue(text.contains("1.1   Introduction"));

        // check include
        assertTrue(text.contains("This set of plugins provides a way to associate a Nuxeo Document with a Template."));
    }

    @Test
    public void testXLrendering() throws Exception {

        PathRef ref = new PathRef("default-domain/workspaces/templateSamples/");
        DocumentModel sampleFolder = session.getDocument(ref);
        assertNotNull(sampleFolder);

        ref = new PathRef("default-domain/workspaces/templateSamples/note4XL");
        DocumentModel note = session.getDocument(ref);

        TemplateBasedDocument noteTemplate = note.getAdapter(TemplateBasedDocument.class);
        assertNotNull(noteTemplate);

        List<String> templateNames = noteTemplate.getTemplateNames();
        assertEquals(1, templateNames.size());
        assertEquals("XL MetaData render", templateNames.get(0));

        Blob blob = noteTemplate.renderWithTemplate("XL MetaData render");
        assertNotNull(blob);
        assertTrue(blob.getFilename().endsWith(".xls"));

        ConversionService cs = Framework.getLocalService(ConversionService.class);

        BlobHolder textBH = cs.convert("xl2text", new SimpleBlobHolder(blob),
                new HashMap<String, Serializable>());
        assertNotNull(textBH);
        String text = textBH.getBlob().getString();

        assertTrue(text.contains("Contributors Administrator Tiry "));
        assertTrue(text.contains("Subjects technology/it human sciences/information"));
        assertTrue(text.contains("Format Html"));
    }

}
