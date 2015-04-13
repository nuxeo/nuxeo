package org.nuxeo.ecm.platform.template.tests;

import static org.junit.Assert.assertTrue;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.TransactionalFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.template.processors.xdocreport.FieldDefinitionGenerator;

@RunWith(FeaturesRunner.class)
@Features({ TransactionalFeature.class, CoreFeature.class })
@RepositoryConfig(cleanup = Granularity.METHOD)
@LocalDeploy("org.nuxeo.template.manager.xdocreport.test:core-types-contrib.xml")
public class TestFieldDefinitionGeneration {

    @Inject
    protected CoreSession session;

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

}
