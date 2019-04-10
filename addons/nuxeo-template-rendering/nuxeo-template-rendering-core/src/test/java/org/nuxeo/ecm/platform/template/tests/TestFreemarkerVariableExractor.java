/*
 * (C) Copyright 2011-2016 Nuxeo SA (http://nuxeo.com/) and others.
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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.nuxeo.runtime.test.NXRuntimeTestCase;
import org.nuxeo.template.fm.FreeMarkerVariableExtractor;

public class TestFreemarkerVariableExractor extends NXRuntimeTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.template.manager.api");
        deployContrib("org.nuxeo.template.manager", "OSGI-INF/templateprocessor-service.xml");
        deployContrib("org.nuxeo.template.manager", "OSGI-INF/templateprocessor-contrib.xml");
    }

    @Test
    public void testExtractor() throws Exception {

        File file = org.nuxeo.common.utils.FileUtils.getResourceFileFromContext("data/testFM.tpl");
        String content = FileUtils.readFileToString(file);

        List<String> vars = FreeMarkerVariableExtractor.extractVariables(content);

        // System.out.println(vars);

        assertTrue(vars.contains("simpleVar"));
        assertTrue(vars.contains("objectVar"));
        assertTrue(vars.contains("dateVar"));

        assertTrue(vars.contains("condVar1"));
        assertTrue(vars.contains("condVar2"));
        assertTrue(vars.contains("condVar3"));
        assertTrue(vars.contains("condVar4"));
        assertTrue(vars.contains("condVar5"));

        assertTrue(vars.contains("items"));
        assertFalse(vars.contains("item"));
        assertTrue(vars.contains("container"));

        assertFalse(vars.contains("internalVar"));
        assertFalse(vars.contains("doc"));
        assertFalse(vars.contains("document"));
        assertFalse(vars.contains("auditEntries"));
        assertFalse(vars.contains("auditEntry"));
        assertFalse(vars.contains("subject"));

        assertFalse(vars.contains("doc['dc:title']"));
        assertFalse(vars.contains("doc['dc:subjects']"));

        assertFalse(vars.contains("functions"));
        assertFalse(vars.contains("core"));
        assertFalse(vars.contains("doc['dc:created']"));
        assertFalse(vars.contains("doc['dc:nature']"));
        assertFalse(vars.contains("doc.dublincore.created"));

        assertFalse(vars.contains("(doc"));
        assertFalse(vars.contains("1..(doc"));

        assertFalse(vars.contains("allergen"));

        assertFalse(vars.contains("subject_index"));
        assertFalse(vars.contains("subject_has_index"));

        assertFalse(vars.contains(".lang"));
    }
}
