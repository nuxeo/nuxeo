package org.nuxeo.ecm.platform.template.tests;

import java.io.File;
import java.util.List;

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.platform.template.processors.fm.FreeMarkerVariableExtractor;

import junit.framework.TestCase;

public class TestFreemarkerVariableExractor extends TestCase {


    public void testExtractor() throws Exception {

        File file = FileUtils.getResourceFileFromContext("data/testFM.tpl");
        String content = FileUtils.readFile(file);

        List<String> vars = FreeMarkerVariableExtractor.extractVariables(content);

        System.out.println(vars);

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

        assertFalse(vars.contains("subject"));


        assertFalse(vars.contains("doc['dc:title']"));
        assertFalse(vars.contains("doc['dc:subjects']"));

    }

}
