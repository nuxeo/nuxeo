/*
 * (C) Copyright 2006-2015 Nuxeo SA (http://nuxeo.com/) and contributors.
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

package org.nuxeo.common.utils;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.junit.Test;

import org.nuxeo.common.Environment;
import org.nuxeo.common.codec.CryptoProperties;

import freemarker.template.TemplateException;

/**
 * @author sfermigier
 */
public class TestTextTemplate {

    @Test
    public void test1() {
        Map<String, String> testVariables = new HashMap<>();
        testVariables.put("var1", "value1");
        testVariables.put("var2", "value2");
        testVariables.put("var3", "value3");
        TextTemplate tt = new TextTemplate(testVariables);
        String templateText = "test ${var1} and ${var2} and ${var3}";
        assertEquals("test value1 and value2 and value3", tt.processText(templateText));
    }

    @Test
    public void testCrypto() throws IOException, TemplateException {
        CryptoProperties props = new CryptoProperties();
        props.setProperty(Environment.CRYPT_KEY,
                org.apache.commons.codec.binary.Base64.encodeBase64String("secret".getBytes()));
        Map<String, String> testVariables = new HashMap<>();
        testVariables.put("var1", "{$$Ab5uGXsjB3DHqBfkn6LKuQ==}"); // "value1"
        testVariables.put("var2", "{$$36YqHApithKHOJ+UkfIDJQ==}"); // "value2"
        testVariables.put("var3", "{$$h+/aMIx9fttICp0g8oLZyw==}"); // "value3"
        TextTemplate tt = new TextTemplate(props);
        tt.setVariables(testVariables);
        String templateText = "test ${var1} and ${var2} and ${var3}";
        assertEquals("test value1 and value2 and value3", tt.processText(templateText));

        File ftl = new File(getClass().getClassLoader().getResource("freemarkerTemplate.ftl").getPath());
        File tmpFile = File.createTempFile("ftl", null);
        tt.processFreemarker(ftl, tmpFile);
        try (BufferedReader reader = new BufferedReader(new FileReader(tmpFile))) {
            assertEquals("test value1 and value2 and value3", reader.readLine());
        }

        tt.setKeepEncryptedAsVar(true);
        assertEquals("Encrypted variables must not be replaced", templateText, tt.processText(templateText));
        tt.processFreemarker(ftl, tmpFile);
        try (BufferedReader reader = new BufferedReader(new FileReader(tmpFile))) {
            assertEquals("Encrypted variables must not be replaced", templateText, reader.readLine());
        }
        tmpFile.delete();
    }

    @Test
    public void test2() {
        Properties vars = new Properties();
        vars.setProperty("k1", "v1");
        TextTemplate tt = new TextTemplate(vars);

        assertEquals(vars.stringPropertyNames(), tt.getVariables().stringPropertyNames());
        assertEquals("v1", tt.getVariable("k1"));

        tt.setVariable("k2", "v2");
        String text = tt.processText("${k1}-${k2}");
        assertEquals("v1-v2", text);
    }

    @Test
    public void testParameterExpansion() {
        TextTemplate emptytt = new TextTemplate(new Properties());
        Properties vars = new Properties();
        vars.setProperty("foo", "bar");
        TextTemplate tt = new TextTemplate(vars);
        assertEquals("baz", emptytt.processText("${foo:=baz}"));
        assertEquals("bar", tt.processText("${foo:=baz}"));
        assertEquals("<foo>${myUnresolvedExpression}</foo>", tt.processText("<foo>${myUnresolvedExpression}</foo>"));
        vars.setProperty("myUnresolvedExpression", "");
        assertEquals("<foo></foo>", tt.processText("<foo>${myUnresolvedExpression}</foo>"));
    }

    @Test
    public void testEscapeVariable() {
        TextTemplate templates = new TextTemplate();
        templates.setVariable("pfouh", "bar");
        assertEquals("bar${pfouh}", templates.processText("${pfouh}$${pfouh}"));
    }

}
