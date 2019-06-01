package org.nuxeo.template;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.template.api.TemplateInput;
import org.nuxeo.template.api.TemplateProcessorService;
import org.nuxeo.template.api.adapters.TemplateBasedDocument;
import org.nuxeo.template.api.adapters.TemplateSourceDocument;
import org.nuxeo.template.processors.fm.TemplateInputUtils;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
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
@Deploy("org.nuxeo.template.manager")
public class TestTemplateRenderingService {

    protected static final String TEMPLATE_NAME = "mytestTemplate";

    protected static final String WEBVIEW_RENDITION = "webView";

    protected static final String JOE_USERNAME = "joe";

    @Inject
    protected CoreSession session;

    @Inject
    protected AutomationService automationService;

    @Inject
    TemplateProcessorService tps;

    protected DocumentModel renditionContainer;

    @Before
    public void setup() {
        renditionContainer = session.createDocumentModel("/", "renditions", "Folder");
        renditionContainer = session.createDocument(renditionContainer);
    }

    @Test
    public void whenTemplateWithoutDynamicPart_shouldRenderBlobAsIt() throws IOException {
        TemplateSourceDocument templateSrc = createTemplateSourceDoc("Hello world !", WEBVIEW_RENDITION);
        TemplateBasedDocument templateBase = createTemplateBasedDoc(templateSrc.getAdaptedDoc());

        Blob result = templateBase.renderWithTemplate(TEMPLATE_NAME);
        assertNotNull(result);
        assertEquals("Hello world !", result.getString());
    }

    @Test
    public void whenTemplateWithUsename_shouldRenderBlobAsIt() throws IOException {
        TemplateSourceDocument templateSrc = createTemplateSourceDoc("Hello ${username} !", WEBVIEW_RENDITION);
        TemplateBasedDocument templateBase = createTemplateBasedDoc(templateSrc.getAdaptedDoc());

        Blob result = templateBase.renderWithTemplate(TEMPLATE_NAME);
        assertNotNull(result);
        assertEquals("Hello Administrator !", result.getString());
    }

    @Test
    public void whenTemplateWithTemplateDocTitle_shouldRenderBlobAsIt() throws IOException {
        TemplateSourceDocument templateSrc = createTemplateSourceDoc("Hello from ${doc['dc:title']} !", WEBVIEW_RENDITION);
        TemplateBasedDocument templateBase = createTemplateBasedDoc(templateSrc.getAdaptedDoc());

        Blob result = templateBase.renderWithTemplate(TEMPLATE_NAME);
        assertNotNull(result);
        assertEquals("Hello from MyTemplateBase !", result.getString());
    }

    @Test
    public void whenTemplateWithParamFromGivenInputTemplate_String_shouldRenderBlobAsIt() throws IOException {
        TemplateSourceDocument templateSrc = createTemplateSourceDoc("We are introducing ${myStringInCtxt} !", WEBVIEW_RENDITION);
        TemplateBasedDocument templateBase = createTemplateBasedDoc(templateSrc.getAdaptedDoc());
        List<TemplateInput> params = Arrays.asList(TemplateInputUtils.createStringTemplateInput("myStringInCtxt", "Beautiful String"));
        templateBase.saveParams(TEMPLATE_NAME, params, true);

        Blob result = templateBase.renderWithTemplate(TEMPLATE_NAME);
        assertNotNull(result);
        assertEquals("We are introducing Beautiful String !", result.getString());
    }

    protected TemplateBasedDocument createTemplateBasedDoc(DocumentModel templateDoc) {

        DocumentModel templateBase = session.createDocumentModel("renditions", "templateBase", "Note");
        templateBase.setPropertyValue("dc:title", "MyTemplateBase");
        templateBase = session.createDocument(templateBase);

        // associate File to template
        templateBase = tps.makeTemplateBasedDocument(templateBase, templateDoc, true);

        return templateBase.getAdapter(TemplateBasedDocument.class);
    }

    protected TemplateSourceDocument createTemplateSourceDoc(String templateString, String targetRendition) {
        return createTemplateSourceDoc(templateString, targetRendition, new ArrayList<>());
    }

    protected TemplateSourceDocument createTemplateSourceDoc(String templateString, String targetRendition, List<TemplateInput> templateInputs) {

        // create the template
        DocumentModel templateSource = session.createDocumentModel("/", "templatedDoc", "TemplateSource");
        templateSource.setPropertyValue("dc:title", "MyTemplateSource");
        templateSource.setPropertyValue("file:content", new StringBlob(templateString, "text/x-freemarker"));
        templateSource.setPropertyValue("tmpl:templateName", TEMPLATE_NAME);

        templateSource = session.createDocument(templateSource);

        // configure targetRendition and output format
        TemplateSourceDocument source = templateSource.getAdapter(TemplateSourceDocument.class);
        //source.setTargetRenditioName(targetRendition, false);

        for (TemplateInput param : templateInputs) {
            source.addInput(param);
        }

        // update the doc and adapter
        templateSource = session.saveDocument(source.getAdaptedDoc());
        session.save();

        return templateSource.getAdapter(TemplateSourceDocument.class);
    }
}
