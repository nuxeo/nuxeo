package org.nuxeo.ecm.platform.template.tests;

import org.junit.Test;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.template.processors.xdocreport.FieldDefinitionGenerator;
import static org.junit.Assert.*;

public class TestFieldDefinitionGeneration extends SQLRepositoryTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.core.api");
        deployBundle("org.nuxeo.ecm.core");
        deployBundle("org.nuxeo.ecm.core.event");
        deployContrib("org.nuxeo.template.manager.xdocreport.test",
                "core-types-contrib.xml");
        openSession();
    }

    @Test
    public void testGeneration() throws Exception {
        String xml = FieldDefinitionGenerator.generate(session.getRootDocument());
        // System.out.println(xml);
        assertTrue(xml.contains("<field name=\"doc.dublincore.subjects\" list=\"true\""));
        assertTrue(xml.contains("<field name=\"doc.dublincore.nature\" list=\"false\""));
    }

    @Test
    public void testFileGeneration() throws Exception {
        String xml = FieldDefinitionGenerator.generate("File");
        // System.out.println(xml);
        assertTrue(xml.contains("<field name=\"doc.file.content\" list=\"false\""));
        assertTrue(xml.contains("<field name=\"doc.file.content.filename\" list=\"false\""));
    }

    @Test
    public void testComplexGeneration() throws Exception {
        String xml = FieldDefinitionGenerator.generate("DocWithComplex");
        // System.out.println(xml);
        assertTrue(xml.contains("<field name=\"doc.testComplex.complex1\" list=\"false\""));
        assertTrue(xml.contains("<field name=\"doc.testComplex.complex1.maximum\" list=\"false\""));
        assertTrue(xml.contains("<field name=\"doc.testComplex.complex1.unit\" list=\"false\""));
    }

    @Override
    public void tearDown() throws Exception {
        EventService eventService = Framework.getLocalService(EventService.class);
        eventService.waitForAsyncCompletion();
        closeSession();
        super.tearDown();
    }

}
