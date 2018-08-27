/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Estelle Giuly <egiuly@nuxeo.com>
 */

package org.nuxeo.ecm.platform.template.pub.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.nuxeo.ecm.platform.rendition.Constants.RENDITION_FACET;
import static org.nuxeo.ecm.platform.rendition.Constants.RENDITION_SCHEMA;
import static org.nuxeo.ecm.platform.rendition.publisher.RenditionPublicationFactory.RENDITION_NAME_PARAMETER_KEY;

import java.io.File;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CloseableCoreSession;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentSecurityException;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.impl.ACLImpl;
import org.nuxeo.ecm.core.api.security.impl.ACPImpl;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.publisher.api.PublicationNode;
import org.nuxeo.ecm.platform.publisher.api.PublicationTree;
import org.nuxeo.ecm.platform.publisher.api.PublisherService;
import org.nuxeo.ecm.platform.publisher.impl.core.SimpleCorePublishedDocument;
import org.nuxeo.ecm.platform.rendition.service.RenditionDefinition;
import org.nuxeo.ecm.platform.rendition.service.RenditionService;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.template.api.InputType;
import org.nuxeo.template.api.TemplateInput;
import org.nuxeo.template.api.TemplateProcessorService;
import org.nuxeo.template.api.adapters.TemplateBasedDocument;
import org.nuxeo.template.api.adapters.TemplateSourceDocument;
import org.nuxeo.template.processors.HtmlBodyExtractor;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(init = RenditionPublicationRepositoryInit.class, cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.ecm.platform.query.api")
@Deploy("org.nuxeo.ecm.platform.convert")
@Deploy("org.nuxeo.ecm.actions")
@Deploy("org.nuxeo.ecm.platform.rendition.api")
@Deploy("org.nuxeo.ecm.platform.rendition.core")
@Deploy("org.nuxeo.ecm.platform.rendition.publisher")
@Deploy("org.nuxeo.ecm.automation.core")
@Deploy("org.nuxeo.ecm.platform.versioning.api")
@Deploy("org.nuxeo.ecm.platform.versioning")
@Deploy("org.nuxeo.ecm.relations")
@Deploy("org.nuxeo.ecm.relations.jena")
@Deploy("org.nuxeo.ecm.platform.publisher.core.contrib")
@Deploy("org.nuxeo.ecm.platform.publisher.core")
@Deploy("org.nuxeo.ecm.platform.publisher.task")
@Deploy("org.nuxeo.ecm.platform.task.core")
@Deploy("org.nuxeo.ecm.platform.task.testing")
@Deploy("org.nuxeo.ecm.platform.rendition.publisher")
@Deploy("org.nuxeo.template.manager")
@Deploy("org.nuxeo.template.manager:relations-default-jena-contrib.xml")
public class TestRenditionPublication {

    protected static final String TEMPLATE_NAME = "mytestTemplate";

    protected static final String WEBVIEW_RENDITION = "webView";

    protected static final String JOE_USERNAME = "joe";

    @Inject
    protected CoreSession session;

    @Inject
    protected PublisherService publisherService;

    @Inject
    protected RenditionService renditionService;

    @Inject
    TemplateProcessorService tps;

    protected DocumentModel createTemplateDoc() throws Exception {

        DocumentModel root = session.getRootDocument();

        // create the template
        DocumentModel templateDoc = session.createDocumentModel(root.getPathAsString(), "templatedDoc",
                "TemplateSource");
        templateDoc.setProperty("dublincore", "title", "MyTemplate");
        File file = FileUtils.getResourceFileFromContext("data/htmlRender.ftl");
        Blob fileBlob = Blobs.createBlob(file);
        fileBlob.setFilename("htmlRendered.ftl");
        templateDoc.setProperty("file", "content", fileBlob);
        templateDoc.setPropertyValue("tmpl:templateName", TEMPLATE_NAME);

        templateDoc = session.createDocument(templateDoc);

        // configure rendition and output format
        TemplateSourceDocument source = templateDoc.getAdapter(TemplateSourceDocument.class);
        source.setTargetRenditioName(WEBVIEW_RENDITION, false);

        // check that parameter has been detected
        assertEquals(1, source.getParams().size());

        // value parameter
        TemplateInput param = new TemplateInput("htmlContent", "htmlPreview");
        param.setType(InputType.Content);
        param.setSource("htmlPreview");
        source.addInput(param);

        // update the doc and adapter
        templateDoc = session.saveDocument(source.getAdaptedDoc());
        source = templateDoc.getAdapter(TemplateSourceDocument.class);

        assertEquals(1, source.getParams().size());
        TemplateInput inputParam = source.getParams().get(0);
        assertEquals("htmlContent", inputParam.getName());
        assertEquals("htmlPreview", inputParam.getSource());

        return templateDoc;
    }

    protected DocumentModel createTemplateBasedDoc(DocumentModel templateDoc) throws Exception {

        DocumentModel root = session.getRootDocument();

        // create the Note
        DocumentModel testDoc = session.createDocumentModel(root.getPathAsString(), "testDoc", "Note");
        testDoc.setProperty("dublincore", "title", "MyTestNoteDoc");
        testDoc.setProperty("dublincore", "description", "Simple note sample");

        testDoc.setProperty("note", "mime_type", "text/html");
        testDoc.setProperty("note", "note", "<html><body><p> Simple <b> Note </b> with <i>text</i></body></html>");

        testDoc = session.createDocument(testDoc);

        // associate File to template
        testDoc = tps.makeTemplateBasedDocument(testDoc, templateDoc, true);

        TemplateBasedDocument templateBased = testDoc.getAdapter(TemplateBasedDocument.class);
        assertNotNull(templateBased);

        return testDoc;
    }

    @Test
    public void verifyRenditionBinding() throws Exception {

        DocumentModel templateBasedDoc = createTemplateBasedDoc(createTemplateDoc());
        TemplateBasedDocument templateBased = templateBasedDoc.getAdapter(TemplateBasedDocument.class);
        assertNotNull(templateBased);

        templateBased.getSourceTemplate(TEMPLATE_NAME).setTargetRenditioName(null, true);

        List<RenditionDefinition> defs = renditionService.getAvailableRenditionDefinitions(templateBasedDoc);
        // one blob => pdf rendition, + export renditions
        assertEquals(4, defs.size());

        templateBased.getSourceTemplate(TEMPLATE_NAME).setTargetRenditioName(WEBVIEW_RENDITION, true);
        defs = renditionService.getAvailableRenditionDefinitions(templateBasedDoc);
        // blob, + delivery rendition binding + export renditions => 5 rendition
        assertEquals(5, defs.size());

    }

    @Test
    public void shouldPublishATemplateRendition() throws Exception {

        // setup tree
        String defaultTreeName = publisherService.getAvailablePublicationTree().get(0);
        PublicationTree tree = publisherService.getPublicationTree(defaultTreeName, session, null);

        List<PublicationNode> nodes = tree.getChildrenNodes();
        assertEquals(1, nodes.size());
        assertEquals("Section1", nodes.get(0).getTitle());

        PublicationNode targetNode = nodes.get(0);
        assertTrue(tree.canPublishTo(targetNode));

        // create a template doc
        DocumentModel templateBasedDoc = createTemplateBasedDoc(createTemplateDoc());

        // publish
        SimpleCorePublishedDocument publishedDocument = (SimpleCorePublishedDocument) tree.publish(templateBasedDoc,
                targetNode, Collections.singletonMap(RENDITION_NAME_PARAMETER_KEY, WEBVIEW_RENDITION));

        // check rendition is done
        DocumentModel proxy = publishedDocument.getProxy();
        assertTrue(proxy.hasFacet(RENDITION_FACET));
        assertTrue(proxy.hasSchema(RENDITION_SCHEMA));

        BlobHolder bh = proxy.getAdapter(BlobHolder.class);
        Blob renditionBlob = bh.getBlob();
        assertNotNull(renditionBlob);

        String htmlPage = renditionBlob.getString();
        assertNotNull(htmlPage);
        System.out.print(htmlPage);
        assertTrue(htmlPage.contains((String) templateBasedDoc.getPropertyValue("dc:description")));
        assertTrue(htmlPage.contains(templateBasedDoc.getTitle()));
        String noteHtmlContent = HtmlBodyExtractor.extractHtmlBody(
                (String) templateBasedDoc.getPropertyValue("note:note"));
        assertNotNull(noteHtmlContent);
        System.out.print(noteHtmlContent);
        assertTrue(htmlPage.contains(noteHtmlContent));

        // verify html body extraction
        int bodyIdx = htmlPage.indexOf("<body>");
        assertTrue(bodyIdx > 0); // at least one body tag
        assertTrue(htmlPage.indexOf("<body>", bodyIdx + 1) < 0); // but not 2

        // note is versioned at creation and for each updates
        // refetch !?
        proxy = session.getDocument(proxy.getRef());
        assertEquals("0.2", proxy.getVersionLabel());

        // update template
        templateBasedDoc.setPropertyValue("dc:description", "updated!");
        templateBasedDoc = session.saveDocument(templateBasedDoc);
        session.save();
        // as it's a note, it is versioned for each updates
        assertEquals("0.3", templateBasedDoc.getVersionLabel());

        // republish
        publishedDocument = (SimpleCorePublishedDocument) tree.publish(templateBasedDoc, targetNode,
                Collections.singletonMap(RENDITION_NAME_PARAMETER_KEY, WEBVIEW_RENDITION));

        proxy = publishedDocument.getProxy();
        assertTrue(proxy.hasFacet(RENDITION_FACET));
        assertTrue(proxy.hasSchema(RENDITION_SCHEMA));

        bh = proxy.getAdapter(BlobHolder.class);
        renditionBlob = bh.getBlob();
        assertNotNull(renditionBlob);

        // refetch !?
        proxy = session.getDocument(proxy.getRef());
        assertEquals("0.3", proxy.getVersionLabel());

    }

    @Test
    public void shouldGetNullTemplateIfAccessToTemplateForbidden() throws Exception {

        // Forbid access to the template doc
        DocumentModel templateDoc = createTemplateDoc();
        ACP acpDeny = new ACPImpl();
        ACL aclDeny = new ACLImpl();
        aclDeny.add(ACE.BLOCK);
        acpDeny.addACL(aclDeny);
        session.setACP(templateDoc.getRef(), acpDeny, true);

        // Authorize access to the template based document
        DocumentModel templateBasedDoc = createTemplateBasedDoc(templateDoc);
        ACP acpGrant = new ACPImpl();
        ACL aclGrant = new ACLImpl();
        aclGrant.add(new ACE(JOE_USERNAME, "Read", true));
        acpGrant.addACL(aclGrant);
        session.setACP(templateBasedDoc.getRef(), acpGrant, true);

        session.save();

        try (CloseableCoreSession joeSession = CoreInstance.openCoreSession(session.getRepositoryName(),
                JOE_USERNAME)) {
            // Check that joe user cannot access the template document
            try {
                joeSession.getDocument(templateDoc.getRef());
                fail("privilege to the template document is granted but should not be");
            } catch (DocumentSecurityException e) {
                // ok
            }

            // Check that joe user can access the template based document
            DocumentModel joeTemplateBasedDoc = joeSession.getDocument(templateBasedDoc.getRef());
            assertNotNull(joeTemplateBasedDoc);
            TemplateBasedDocument joeTemplateBased = joeTemplateBasedDoc.getAdapter(TemplateBasedDocument.class);
            assertNull(joeTemplateBased.getSourceTemplate(TEMPLATE_NAME));
            assertNull(joeTemplateBased.getTemplateNameForRendition(WEBVIEW_RENDITION));
        }
    }
}
