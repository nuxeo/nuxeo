/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thierry Delprat
 */
package org.nuxeo.ecm.platform.template.tests;

import static org.junit.Assert.assertTrue;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.template.processors.xdocreport.FieldDefinitionGenerator;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.template.manager.xdocreport.test:core-types-contrib.xml")
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
