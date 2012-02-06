package org.nuxeo.ecm.platform.template.tests;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.platform.template.processors.TemplateProcessor;
import org.nuxeo.ecm.platform.template.processors.docx.WordXMLRawTemplateProcessor;
import org.nuxeo.ecm.platform.template.processors.xdocreport.XDocReportProcessor;
import org.nuxeo.ecm.platform.template.service.TemplateProcessorComponent;
import org.nuxeo.ecm.platform.template.service.TemplateProcessorDescriptor;
import org.nuxeo.ecm.platform.template.service.TemplateProcessorService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

public class TestService extends NXRuntimeTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployContrib("org.nuxeo.ecm.platform.template.manager",
                "OSGI-INF/templateprocessor-service.xml");
    }

    public void testServiceLookup() {
        TemplateProcessorService tps = Framework.getLocalService(TemplateProcessorService.class);
        assertNotNull(tps);
    }

    public void testRegisterMergeUnRegisterContrib() throws Exception {

        // test simple registration
        deployContrib("org.nuxeo.ecm.platform.template.manager.test",
                "OSGI-INF/templateprocessor-contrib.xml");

        TemplateProcessorService tps = Framework.getLocalService(TemplateProcessorService.class);

        assertNotNull(tps.getProcessor("rawWordXML"));

        TemplateProcessorDescriptor desc = ((TemplateProcessorComponent) tps).getDescriptor("rawWordXML");

        assertEquals("rawWordXML", desc.getName());
        assertEquals("Raw Word XML", desc.getLabel());
        assertEquals(WordXMLRawTemplateProcessor.class.getSimpleName(),
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
        deployContrib("org.nuxeo.ecm.platform.template.manager.test",
                "OSGI-INF/templateprocessor-contrib2.xml");

        assertNotNull(tps.getProcessor("rawWordXML"));
        desc = ((TemplateProcessorComponent) tps).getDescriptor("rawWordXML");
        assertEquals("rawWordXML", desc.getName());
        assertEquals("Raw Word XML", desc.getLabel());
        assertEquals(WordXMLRawTemplateProcessor.class.getSimpleName(),
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
        undeployContrib("org.nuxeo.ecm.platform.template.manager.test",
                "OSGI-INF/templateprocessor-contrib2.xml");

        fakeBlob.setFilename("bidon.bidon");
        assertNull(tps.findProcessor(fakeBlob));

        fakeBlob.setFilename("bidon.nawak");
        assertNotNull(tps.findProcessor(fakeBlob));

    }

    public void testDefaultContrib() throws Exception {

        // test simple registration
        deployContrib("org.nuxeo.ecm.platform.template.manager",
                "OSGI-INF/templateprocessor-contrib.xml");

        TemplateProcessorService tps = Framework.getLocalService(TemplateProcessorService.class);

        // check that the 3 processors are registred
        assertNotNull(tps.getProcessor("rawWordXML"));
        assertNotNull(tps.getProcessor("JODReportProcessor"));
        assertNotNull(tps.getProcessor("XDocReportProcessor"));

        Blob fakeBlob = new StringBlob("Empty");
        fakeBlob.setFilename("bidon.docx");

        TemplateProcessor processor = tps.findProcessor(fakeBlob);
        assertNotNull(processor);

        assertEquals(XDocReportProcessor.class.getSimpleName(),
                processor.getClass().getSimpleName());

        fakeBlob.setFilename("bidon.odt");

        processor = tps.findProcessor(fakeBlob);
        assertNotNull(processor);

        assertEquals(XDocReportProcessor.class.getSimpleName(),
                processor.getClass().getSimpleName());

    }

}
