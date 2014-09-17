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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolder;
import org.nuxeo.ecm.core.convert.api.ConversionService;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.BackendType;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.template.api.adapters.TemplateBasedDocument;

import com.google.inject.Inject;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(type = BackendType.H2, user = "Administrator", cleanup = Granularity.CLASS)
@Deploy({ "org.nuxeo.ecm.platform.content.template",
        "org.nuxeo.ecm.core.event", "org.nuxeo.ecm.core.convert.api",
        "org.nuxeo.ecm.platform.mimetype.api",
        "org.nuxeo.ecm.platform.mimetype.core", "org.nuxeo.ecm.core.convert",
        "org.nuxeo.ecm.core.convert.plugins", "org.nuxeo.ecm.platform.convert",
        "org.nuxeo.ecm.platform.preview", "org.nuxeo.ecm.platform.dublincore",
        "org.nuxeo.template.manager.api", "org.nuxeo.template.manager",
        "org.nuxeo.template.manager.jaxrs",
        "org.nuxeo.template.manager.xdocreport",
        "org.nuxeo.template.manager.jxls", "org.nuxeo.template.manager.samples" })
public class TestImportedModelRendering {

    DocumentModel rootDocument;

    DocumentModel workspace;

    DocumentModel docToExport;

    @Inject
    protected CoreSession session;

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

        // check TOC (well, content: spaces vary within the TOC)
        String checkedText = "1 Overview";
        assertTrue(String.format("Expecting text '%s' inside '%s'",
                checkedText, text), text.contains(checkedText));
        checkedText = "1.1 Introduction";
        assertTrue(String.format("Expecting text '%s' inside '%s'",
                checkedText, text), text.contains(checkedText));

        // remove "unbreakable spaces"
        text = text.replaceAll("\\u00A0", " ");
        
        

        // check include
        checkedText = "This set of plugins provides a way to "
                + "associate a Nuxeo Document with a Template.";
        assertTrue(String.format("Expecting text '%s' inside '%s'",
                checkedText, text), text.contains(checkedText));
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

    @Test
    public void testInterventionStatement() throws Exception {

        PathRef ref = new PathRef("default-domain/workspaces/templateSamples/");
        DocumentModel sampleFolder = session.getDocument(ref);
        assertNotNull(sampleFolder);

        ref = new PathRef(
                "default-domain/workspaces/templateSamples/intervention");
        DocumentModel intervention = session.getDocument(ref);

        TemplateBasedDocument interventionTemplate = intervention.getAdapter(TemplateBasedDocument.class);
        assertNotNull(interventionTemplate);

        List<String> templateNames = interventionTemplate.getTemplateNames();
        assertEquals(1, templateNames.size());
        assertEquals("Delivery Statement", templateNames.get(0));

        Blob blob = interventionTemplate.renderWithTemplate("Delivery Statement");
        assertNotNull(blob);
        assertTrue(blob.getFilename().endsWith(".pdf"));

        ConversionService cs = Framework.getLocalService(ConversionService.class);

        BlobHolder textBH = cs.convertToMimeType("text/plain",
                new SimpleBlobHolder(blob), new HashMap<String, Serializable>());
        assertNotNull(textBH);
        String text = textBH.getBlob().getString();

        assertTrue(text.contains("2012"));
        assertTrue(text.contains("Freeman"));
        assertTrue(text.contains("Poissonniers"));
    }
}
