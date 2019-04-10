/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

import javax.inject.Inject;

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
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandLineExecutorService;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.template.api.adapters.TemplateBasedDocument;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy({ "org.nuxeo.ecm.platform.content.template", "org.nuxeo.ecm.automation.core", "org.nuxeo.ecm.platform.convert",
    "org.nuxeo.ecm.platform.preview", "org.nuxeo.ecm.platform.dublincore", "org.nuxeo.template.manager.api",
    "org.nuxeo.template.manager", "org.nuxeo.template.manager.jaxrs", "org.nuxeo.template.manager.xdocreport",
    "org.nuxeo.template.manager.jxls", "org.nuxeo.template.manager.samples", "org.nuxeo.ecm.core.io",
    "studio.extensions.template-module-demo", "org.nuxeo.ecm.platform.commandline.executor" })
public class TestImportedModelRendering {

  DocumentModel rootDocument;

  DocumentModel workspace;

  DocumentModel docToExport;

  @Inject
  protected CoreSession session;

  @Inject
  protected ConversionService cs;

  @Inject
  protected CommandLineExecutorService commandLineExecutorService;

  @Test
  public void testNote4Web() throws Exception {

    PathRef ref = new PathRef("/default-domain/workspaces/templatesamples/rawsamples/");
    DocumentModel sampleFolder = session.getDocument(ref);
    assertNotNull(sampleFolder);

    ref = new PathRef("/default-domain/workspaces/templatesamples/rawsamples/webnote");
    DocumentModel note4Web = session.getDocument(ref);

    TemplateBasedDocument note4WebTemplate = note4Web.getAdapter(TemplateBasedDocument.class);
    assertNotNull(note4WebTemplate);

    List<String> templateNames = note4WebTemplate.getTemplateNames();
    assertEquals(1, templateNames.size());
    assertEquals("WebTemplate4Note", templateNames.get(0));

    Blob blob = note4WebTemplate.renderWithTemplate("WebTemplate4Note");
    assertNotNull(blob);
    String htmlContent = blob.getString();
    assertTrue(
        htmlContent.contains("<link class=\"component\" href=\"/nuxeo/site/templates/doc/" + note4Web.getId() + "/"));
    assertTrue(htmlContent.contains("<title> Note4Web </title>"));
    assertTrue(htmlContent.contains("<img src=\"/nuxeo/nxfile/test/" + note4Web.getId() + "/blobholder:1/"));
  }

  @Test
  public void testSampleNote() throws Exception {

    PathRef ref = new PathRef("/default-domain/workspaces/templatesamples/rawsamples/");
    DocumentModel sampleFolder = session.getDocument(ref);
    assertNotNull(sampleFolder);

    ref = new PathRef("/default-domain/workspaces/templatesamples/rawsamples/note");
    DocumentModel note = session.getDocument(ref);

    TemplateBasedDocument noteTemplate = note.getAdapter(TemplateBasedDocument.class);
    assertNotNull(noteTemplate);

    List<String> templateNames = noteTemplate.getTemplateNames();
    assertEquals(1, templateNames.size());
    assertEquals("Note Wrapper", templateNames.get(0));

    Blob blob = noteTemplate.renderWithTemplate("Note Wrapper");
    assertNotNull(blob);
    assertTrue(blob.getFilename().endsWith(".pdf"));

    BlobHolder textBH = cs.convertToMimeType("text/plain", new SimpleBlobHolder(blob),
        new HashMap<String, Serializable>());
    assertNotNull(textBH);
    String text = textBH.getBlob().getString();

    // check TOC (well, content: spaces vary within the TOC)
    String checkedText = "1 Overview";
    assertTrue(String.format("Expecting text '%s' inside '%s'", checkedText, text), text.contains(checkedText));
    checkedText = "1.1 Introduction";
    assertTrue(String.format("Expecting text '%s' inside '%s'", checkedText, text), text.contains(checkedText));

    // remove "unbreakable spaces"
    text = text.replaceAll("\\u00A0", " ");

    // check include
    checkedText = "This set of plugins provides a way to " + "associate a Nuxeo Document with a Template.";
    assertTrue(String.format("Expecting text '%s' inside '%s'", checkedText, text), text.contains(checkedText));
  }

  @Test
  public void testXLrendering() throws Exception {

    PathRef ref = new PathRef("/default-domain/workspaces/templatesamples/rawsamples/");
    DocumentModel sampleFolder = session.getDocument(ref);
    assertNotNull(sampleFolder);

    ref = new PathRef("/default-domain/workspaces/templatesamples/rawsamples/note4XL");
    DocumentModel note = session.getDocument(ref);

    TemplateBasedDocument noteTemplate = note.getAdapter(TemplateBasedDocument.class);
    assertNotNull(noteTemplate);

    List<String> templateNames = noteTemplate.getTemplateNames();
    assertEquals(1, templateNames.size());
    assertEquals("XL MetaData render", templateNames.get(0));

    Blob blob = noteTemplate.renderWithTemplate("XL MetaData render");
    assertNotNull(blob);
    assertTrue(blob.getFilename().endsWith(".xls"));

    BlobHolder textBH = cs.convert("xl2text", new SimpleBlobHolder(blob), new HashMap<String, Serializable>());
    assertNotNull(textBH);
    String text = textBH.getBlob().getString();

    assertTrue(text.contains("Contributors Administrator"));
    assertTrue(text.contains("Subjects technology/it human sciences/information"));
    assertTrue(text.contains("Format Html"));
  }
  
}
