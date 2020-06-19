/*
 * (C) Copyright 2011-2020 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 *     Anahide Tchertchian
 */
package org.nuxeo.apidoc.test;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.nuxeo.apidoc.documentation.SecureXMLHelper;

public class TestSecureXML {

    @Test
    public void testSecureXMLNode() throws Exception {
        assertEquals("<password>********</password>", SecureXMLHelper.secure("<password>p1</password>"));
    }

    @Test
    public void testSecureXMLNodeSpace() throws Exception {
        assertEquals("<password >********</password>", SecureXMLHelper.secure("<password >p1</password>"));
    }

    @Test
    public void testSecureXMLStartNode() throws Exception {
        assertEquals("<passwordExample>********</passwordExample>",
                SecureXMLHelper.secure("<passwordExample>password</passwordExample>"));
    }

    @Test
    public void testSecureXMLEndNode() throws Exception {
        assertEquals("<examplePassword>********</examplePassword>",
                SecureXMLHelper.secure("<examplePassword>password</examplePassword>"));
    }

    @Test
    public void testSecureXMLWhitelistedNode() throws Exception {
        assertEquals("<passwordField>password</passwordField>",
                SecureXMLHelper.secure("<passwordField>password</passwordField>"));
    }

    @Test
    public void testSecureXMLAttribute() throws Exception {
        assertEquals("<prop name=\"password\">********</prop>",
                SecureXMLHelper.secure("<prop name=\"password\">password</prop>"));
    }

    @Test
    public void testSecureXMLAttributSpace() throws Exception {
        assertEquals("<prop name=\"password\" >********</prop>",
                SecureXMLHelper.secure("<prop name=\"password\" >password</prop>"));
    }

    @Test
    public void testSecureXMLAttribute2() throws Exception {
        assertEquals("<prop password=\"********\">foo</prop>",
                SecureXMLHelper.secure("<prop password=\"password\">foo</prop>"));
    }

    @Test
    public void testSecureXMLAttribute3() throws Exception {
        assertEquals("<prop password=\"********\" foo=\"bar\" />",
                SecureXMLHelper.secure("<prop password=\"password\" foo=\"bar\" />"));
    }

    @Test
    public void testSecureXMLStartAttribute() throws Exception {
        assertEquals("<prop name=\"passwordExample\">********</prop>",
                SecureXMLHelper.secure("<prop name=\"passwordExample\">password</prop>"));
    }

    @Test
    public void testSecureXMLStartAttributeSpace() throws Exception {
        assertEquals("<prop name=\"passwordExample\" >********</prop>",
                SecureXMLHelper.secure("<prop name=\"passwordExample\" >password</prop>"));
    }

    @Test
    public void testSecureXMLStartAttribute2() throws Exception {
        assertEquals("<prop passwordExample=\"********\">********</prop>",
                SecureXMLHelper.secure("<prop passwordExample=\"password\">foo</prop>"));
    }

    @Test
    public void testSecureXMLStartAttribute3() throws Exception {
        assertEquals("<prop passwordExample=\"********\" foo=\"bar\">baz</prop>",
                SecureXMLHelper.secure("<prop passwordExample=\"password\" foo=\"bar\">baz</prop>"));
    }

    @Test
    public void testSecureXMLEndAttribute() throws Exception {
        assertEquals("<prop name=\"passwordExample\">********</prop>",
                SecureXMLHelper.secure("<prop name=\"passwordExample\">password</prop>"));
    }

    @Test
    public void testSecureXMLEndAttributeSpace() throws Exception {
        assertEquals("<prop name=\"passwordExample\" >********</prop>",
                SecureXMLHelper.secure("<prop name=\"passwordExample\" >password</prop>"));
    }

    @Test
    public void testSecureXMLEndAttribute2() throws Exception {
        assertEquals("<prop passwordExample=\"********\">********</prop>",
                SecureXMLHelper.secure("<prop passwordExample=\"password\">foo</prop>"));
    }

    @Test
    public void testSecureXMLEndAttribute3() throws Exception {
        assertEquals("<prop passwordExample=\"********\" foo=\"bar\" />",
                SecureXMLHelper.secure("<prop passwordExample=\"password\" foo=\"bar\" />"));
    }

    @Test
    public void testSecureXMLWhitelistedAttribute() throws Exception {
        assertEquals("<prop name=\"passwordField\">password</prop>",
                SecureXMLHelper.secure("<prop name=\"passwordField\">password</prop>"));
    }

    @Test
    public void testSecureXMLWhitelistedAttributeSpace() throws Exception {
        assertEquals("<prop name=\"passwordField\" >password</prop>",
                SecureXMLHelper.secure("<prop name=\"passwordField\" >password</prop>"));
    }

    @Test
    public void testSecureXMLWhitelistedAttribute2() throws Exception {
        assertEquals("<prop passwordField=\"password\">********</prop>",
                SecureXMLHelper.secure("<prop passwordField=\"password\">foo</prop>"));
    }

    @Test
    public void testSecureXMLWhitelistedAttribute3() throws Exception {
        assertEquals("<prop passwordField=\"password\" foo=\"bar\"/>",
                SecureXMLHelper.secure("<prop passwordField=\"password\" foo=\"bar\"/>"));
    }

    @Test
    public void testSecureXML() throws Exception {
        String xml = "foo <password>p1</password>\n" //
                + " <myPassword>p2</myPassword>\n" //
                + " <myPassword >p2</myPassword>\n" //
                + " <yo password=\"p3\" other=\"bla\">\n" //
                + " <yo otherPassword=\"p4\" other=\"bla\">\n" //
                + " <prop name=\"password\">p5</prop>\n" //
                + " <prop name=\"realPassword\">p6</prop>\n" //
                + " <prop name=\"realPassword\" >p6</prop>\n" //
                + " <prop name=\"passwordNotWhitelisted\">p7</prop>\n" //
                + " <prop name=\"passwordField\">ok</prop>\n" //
                + " <secret>${nuxeo.jwt.secret}</secret>\n" //
                + " <secretKey>${nuxeo.aws.secretKey}</secretKey>\n" //
                + " <option name=\"apiKey\">${metrics.datadog.apiKey}</option>\n" //
                + " <passwordField>password</passwordField>\n" //
                + " <passwordHashAlgorithm>SSHA</passwordHashAlgorithm>";
        String expected = "foo <password>********</password>\n" //
                + " <myPassword>********</myPassword>\n" //
                + " <myPassword >********</myPassword>\n" //
                + " <yo password=\"********\" other=\"bla\">\n" //
                + " <yo otherPassword=\"********\" other=\"bla\">\n" //
                + " <prop name=\"password\">********</prop>\n" //
                + " <prop name=\"realPassword\">********</prop>\n" //
                + " <prop name=\"realPassword\" >********</prop>\n" //
                + " <prop name=\"passwordNotWhitelisted\">********</prop>\n" //
                + " <prop name=\"passwordField\">ok</prop>\n" //
                + " <secret>********</secret>\n" //
                + " <secretKey>********</secretKey>\n" //
                + " <option name=\"apiKey\">********</option>\n" //
                + " <passwordField>password</passwordField>\n" //
                + " <passwordHashAlgorithm>SSHA</passwordHashAlgorithm>";
        assertEquals(expected, SecureXMLHelper.secure(xml));
    }

}
