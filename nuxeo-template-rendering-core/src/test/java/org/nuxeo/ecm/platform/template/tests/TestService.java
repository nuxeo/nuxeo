package org.nuxeo.ecm.platform.template.tests;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;
import org.nuxeo.template.api.TemplateProcessor;
import org.nuxeo.template.api.TemplateProcessorService;
import org.nuxeo.template.api.descriptor.ContextExtensionFactoryDescriptor;
import org.nuxeo.template.api.descriptor.OutputFormatDescriptor;
import org.nuxeo.template.api.descriptor.TemplateProcessorDescriptor;
import org.nuxeo.template.processors.fm.FreeMarkerProcessor;
import org.nuxeo.template.processors.xslt.XSLTProcessor;
import org.nuxeo.template.service.TemplateProcessorComponent;

import static org.junit.Assert.*;

public class TestService extends NXRuntimeTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.template.manager.api");
        deployContrib("org.nuxeo.template.manager",
                "OSGI-INF/templateprocessor-service.xml");
    }

    @Test
    public void testServiceLookup() {
        TemplateProcessorService tps = Framework.getLocalService(TemplateProcessorService.class);
        assertNotNull(tps);
    }

    @Test
    public void testRegisterMergeUnRegisterContrib() throws Exception {

        // test simple registration
        deployContrib("org.nuxeo.template.manager.test",
                "OSGI-INF/templateprocessor-contrib.xml");

        TemplateProcessorService tps = Framework.getLocalService(TemplateProcessorService.class);

        assertNotNull(tps.getProcessor("TestProcessor"));

        TemplateProcessorDescriptor desc = ((TemplateProcessorComponent) tps).getDescriptor("TestProcessor");

        assertEquals("TestProcessor", desc.getName());
        assertEquals("Test Processor", desc.getLabel());
        assertEquals(FreeMarkerProcessor.class.getSimpleName(),
                desc.getProcessor().getClass().getSimpleName());
        assertEquals(false, desc.isDefaultProcessor());

        Blob fakeBlob = new StringBlob("Empty");

        fakeBlob.setFilename("bidon.docx");
        assertNotNull(tps.findProcessor(fakeBlob));

        fakeBlob.setFilename("bidon.nawak");
        assertNotNull(tps.findProcessor(fakeBlob));

        fakeBlob.setFilename("bidon.bidon");
        assertNull(tps.findProcessor(fakeBlob));

        fakeBlob.setMimeType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        assertNotNull(tps.findProcessor(fakeBlob));

        fakeBlob.setMimeType("application/nawak");
        assertNotNull(tps.findProcessor(fakeBlob));

        fakeBlob.setMimeType("application/bidon");
        assertNull(tps.findProcessor(fakeBlob));

        // test merge registration
        deployContrib("org.nuxeo.template.manager.test",
                "OSGI-INF/templateprocessor-contrib2.xml");

        assertNotNull(tps.getProcessor("TestProcessor"));
        desc = ((TemplateProcessorComponent) tps).getDescriptor("TestProcessor");
        assertEquals("TestProcessor", desc.getName());
        assertEquals("Test Processor", desc.getLabel());
        assertEquals(FreeMarkerProcessor.class.getSimpleName(),
                desc.getProcessor().getClass().getSimpleName());
        assertEquals(true, desc.isDefaultProcessor()); // Rest the default flag
                                                       // !

        fakeBlob.setFilename("bidon.docx");
        fakeBlob.setMimeType("");
        assertNotNull(tps.findProcessor(fakeBlob));

        fakeBlob.setFilename("bidon.nawak");
        assertNull(tps.findProcessor(fakeBlob));

        fakeBlob.setFilename("bidon.bidon");
        assertNotNull(tps.findProcessor(fakeBlob));

        // check undeploy
        undeployContrib("org.nuxeo.template.manager.test",
                "OSGI-INF/templateprocessor-contrib2.xml");

        fakeBlob.setFilename("bidon.bidon");
        assertNull(tps.findProcessor(fakeBlob));

        fakeBlob.setFilename("bidon.nawak");
        assertNotNull(tps.findProcessor(fakeBlob));

    }

    @Test
    public void testDefaultContrib() throws Exception {

        // test simple registration
        deployContrib("org.nuxeo.template.manager",
                "OSGI-INF/templateprocessor-contrib.xml");

        TemplateProcessorService tps = Framework.getLocalService(TemplateProcessorService.class);

        // check that the 2 default processors are registered
        assertNotNull(tps.getProcessor("Freemarker"));
        assertNotNull(tps.getProcessor("XSLTProcessor"));

        Blob fakeBlob = new StringBlob("Empty");
        fakeBlob.setFilename("bidon.ftl");

        TemplateProcessor processor = tps.findProcessor(fakeBlob);
        assertNotNull(processor);

        assertEquals(FreeMarkerProcessor.class.getSimpleName(),
                processor.getClass().getSimpleName());

        fakeBlob.setFilename("bidon.xml");

        processor = tps.findProcessor(fakeBlob);
        assertNotNull(processor);

        assertEquals(XSLTProcessor.class.getSimpleName(),
                processor.getClass().getSimpleName());

        Collection<TemplateProcessorDescriptor> processors = tps.getRegisteredTemplateProcessors();
        TemplateProcessorDescriptor processorDesc = processors.iterator().next();
        assertNotNull(processorDesc);
        assertTrue(processorDesc.getSupportedMimeTypes().size() > 0);

        // test the default Extensions
        Map<String, ContextExtensionFactoryDescriptor> extensions = tps.getRegistredContextExtensions();
        assertEquals(3, extensions.size());

        ContextExtensionFactoryDescriptor functions = extensions.get("functions");
        assertNotNull(functions);

        List<String> aliases = functions.getAliases();
        assertEquals(2, aliases.size());

        Collection<OutputFormatDescriptor> outFormats = tps.getOutputFormats();
        assertNotNull(outFormats);
        assertFalse(outFormats.isEmpty());
        assertEquals(4, outFormats.size());

        OutputFormatDescriptor pdfDesc = tps.getOutputFormatDescriptor("pdf");
        assertNotNull(pdfDesc);
        assertEquals("PDF", pdfDesc.getLabel());
        assertEquals("application/pdf", pdfDesc.getMimeType());
        assertNull(pdfDesc.getChainId());
        assertTrue(pdfDesc.isEnabled());
    }

}
