/*
 * (C) Copyright 2006-2015 Nuxeo SA (http://nuxeo.com/) and others.
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
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.nuxeo.common.Environment;
import org.nuxeo.common.codec.CryptoProperties;

import freemarker.template.TemplateException;

/**
 * @author sfermigier
 */
public class TestTextTemplate {

    private final String templateText = "test ${var1} and ${var2} and ${var3}";

    private final String processedText = "test value1 and value2 and value3";

    private final String templateText2 = "test ${var.decrypt.var1}"; // will value ${#var1}

    private final String templateText2bis = "test ${#var1}"; // that format is not usable in FreeMarker

    private final String processedText2 = "test value1";

    @Rule
    public TemporaryFolder temporary = new TemporaryFolder();

    @Test
    public void test1() {
        Map<String, String> testVariables = new HashMap<>();
        testVariables.put("var1", "value1");
        testVariables.put("var2", "value2");
        testVariables.put("var3", "value3");
        testVariables.put("var.decrypt.var1", "${#var1}");
        TextTemplate tt = new TextTemplate(testVariables);
        assertEquals(processedText, tt.processText(templateText));
        assertEquals(processedText2, tt.processText(templateText2));
        assertEquals(processedText2, tt.processText(templateText2bis));
    }

    private TextTemplate getTextTemplateWithCryptoVariables() {
        CryptoProperties props = new CryptoProperties();
        props.setProperty(Environment.CRYPT_KEY,
                org.apache.commons.codec.binary.Base64.encodeBase64String("secret".getBytes()));
        Map<String, String> testVariables = new HashMap<>();
        testVariables.put("var1", "{$$Ab5uGXsjB3DHqBfkn6LKuQ==}"); // "value1"
        testVariables.put("var2", "{$$36YqHApithKHOJ+UkfIDJQ==}"); // "value2"
        testVariables.put("var3", "{$$h+/aMIx9fttICp0g8oLZyw==}"); // "value3"
        testVariables.put("var.decrypt.var1", "${#var1}");
        TextTemplate tt = new TextTemplate(props);
        tt.setVariables(testVariables);
        return tt;
    }

    @Test
    public void testCryptoText() {
        TextTemplate tt = getTextTemplateWithCryptoVariables();
        assertEquals(processedText, tt.processText(templateText));
        assertEquals(processedText2, tt.processText(templateText2));
        assertEquals(processedText2, tt.processText(templateText2bis));

        tt.setKeepEncryptedAsVar(true);
        assertEquals("Encrypted variables must not be replaced", templateText, tt.processText(templateText));
        assertEquals(processedText2, tt.processText(templateText2));
        assertEquals(processedText2, tt.processText(templateText2bis));
    }

    @Test
    public void testCryptoFreemarker() throws IOException, TemplateException {
        TextTemplate tt = getTextTemplateWithCryptoVariables();
        File ftl = FileUtils.getResourceFileFromContext("freemarkerTemplate.ftl");
        File tmpFile = temporary.newFile("ftl");
        tt.processFreemarker(ftl, tmpFile);
        try (BufferedReader reader = new BufferedReader(new FileReader(tmpFile))) {
            assertEquals(processedText, reader.readLine());
            assertEquals(processedText2, reader.readLine());
        }

        tt.setKeepEncryptedAsVar(true);
        tt.processFreemarker(ftl, tmpFile);
        try (BufferedReader reader = new BufferedReader(new FileReader(tmpFile))) {
            assertEquals("Encrypted variables must be decrypted", processedText, reader.readLine());
            assertEquals("Encrypted #variables must be replaced", processedText2, reader.readLine());
        }
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
        // Empty TextTemplate
        TextTemplate tt = new TextTemplate();
        assertEquals("baz", tt.processText("${foo:=baz}"));
        Properties vars = new Properties();
        vars.setProperty("foo", "bar");
        // TextTemplate with var "foo"
        tt = new TextTemplate(vars);
        assertEquals("bar", tt.processText("${foo:=baz}"));
        assertEquals("<foo>${myUnresolvedExpression}</foo>",
                tt.processText("<foo>${myUnresolvedExpression}</foo>"));
        vars.setProperty("myUnresolvedExpression", "");
        assertEquals("<foo></foo>", tt.processText("<foo>${myUnresolvedExpression}</foo>"));
    }

    @Test
    public void testEscapeVariable() {
        TextTemplate templates = new TextTemplate();
        templates.setVariable("pfouh", "bar");
        assertEquals("bar${pfouh}", templates.processText("${pfouh}$${pfouh}"));
        // $$ unchanged if not in front of a {
        assertEquals("$mypass", templates.processText("$mypass"));
        assertEquals("my$pass", templates.processText("my$pass"));
        assertEquals("mypass$", templates.processText("mypass$"));
        assertEquals("$$mypass", templates.processText("$$mypass"));
        assertEquals("my$$pass", templates.processText("my$$pass"));
        assertEquals("mypass$$", templates.processText("mypass$$"));
        // $$ unchanged if used in a crypto value (cf org.nuxeo.common.codec.Crypto.CRYPTO_PATTERN)
        assertEquals("{$$CZjbsiX748UF583qkbinsQ==}", templates.processText("{$$CZjbsiX748UF583qkbinsQ==}"));
    }

    @Test
    public void testFreetemplateFileNameValidity() throws Exception {

        File tmpdir = temporary.newFolder();

        TextTemplate textTemplate = new TextTemplate();
        textTemplate.setFreemarkerParsingExtensions("nxftl");
        File testResouceDirectory = FileUtils.getResourceFileFromContext("test-nxftl");

        textTemplate.processDirectory(new File(testResouceDirectory, "tutu.xml.nxftl"), tmpdir);
        textTemplate.processDirectory(new File(testResouceDirectory, "tutu.nxftl"), tmpdir);

        File failingFile = new File(testResouceDirectory, ".nxftl");

        try {
            textTemplate.processDirectory(failingFile, tmpdir);
            fail("File name is invalid the call should not success");
        } catch (IOException expected) {
            String expectedMessage = "Extension only as a filename is not allowed: "
                    + failingFile.getAbsolutePath();
            assertEquals(expectedMessage, expected.getMessage());
        }

    }

}
