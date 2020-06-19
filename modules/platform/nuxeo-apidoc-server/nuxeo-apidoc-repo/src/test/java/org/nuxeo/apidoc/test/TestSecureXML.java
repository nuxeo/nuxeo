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
import static org.junit.Assert.fail;

import java.util.List;

import javax.xml.stream.XMLStreamException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.apidoc.documentation.SecureXMLHelper;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
@Deploy("org.nuxeo.apidoc.repo:OSGI-INF/snapshot-service-framework.xml")
public class TestSecureXML {

    protected void check(String expected, String xml) {
        checkStAX(expected, xml);
        checkRegexp(expected, xml);
        // check fallback
        assertEquals(expected, SecureXMLHelper.secure(xml));
    }

    protected void checkStAX(String expected, String xml) {
        try {
            assertEquals(expected, SecureXMLHelper.secureStAX(xml, SecureXMLHelper.getKeywords(),
                    SecureXMLHelper.getWhitelistedKeywords()));
        } catch (XMLStreamException e) {
            fail(e.getMessage());
        }
    }

    protected void checkRegexp(String expected, String xml) {
        assertEquals(expected, SecureXMLHelper.secureRegexp(xml, SecureXMLHelper.getKeywords(),
                SecureXMLHelper.getWhitelistedKeywords()));
    }

    @Test
    public void testSecureXMLNode() throws Exception {
        check("<password>********</password>", "<password>p1</password>");
    }

    @Test
    public void testSecureXMLNodeDeclaration() throws Exception {
        check("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n<password>********</password>",
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n<password>p1</password>");
    }

    @Test
    public void testSecureXMLNodeSpace() throws Exception {
        String xml = "<password >p1</password>";
        checkStAX("<password>********</password>", xml);
        checkRegexp("<password >********</password>", xml);
    }

    @Test
    public void testSecureXMLStartNode() throws Exception {
        check("<passwordExample>********</passwordExample>", "<passwordExample>password</passwordExample>");
    }

    @Test
    public void testSecureXMLEndNode() throws Exception {
        check("<examplePassword>********</examplePassword>", "<examplePassword>password</examplePassword>");
    }

    @Test
    public void testSecureXMLWhitelistedNode() throws Exception {
        check("<passwordField>password</passwordField>", "<passwordField>password</passwordField>");
    }

    @Test
    public void testSecureXMLAttribute() throws Exception {
        check("<prop name=\"password\">********</prop>", "<prop name=\"password\">password</prop>");
    }

    @Test
    public void testSecureXMLAttributSpace() throws Exception {
        String xml = "<prop name=\"password\" >password</prop>";
        checkStAX("<prop name=\"password\">********</prop>", xml);
        checkRegexp("<prop name=\"password\" >********</prop>", xml);
    }

    @Test
    public void testSecureXMLAttribute2() throws Exception {
        String xml = "<prop password=\"password\">foo</prop>";
        checkStAX("<prop password=\"********\">********</prop>", xml);
        checkRegexp("<prop password=\"********\">foo</prop>", xml);
    }

    @Test
    public void testSecureXMLAttribute2SpaceAndTabs() throws Exception {
        String xml = "<prop password = \t\t \"password\"    >foo</prop>";
        checkStAX("<prop password=\"********\">********</prop>", xml);
        checkRegexp("<prop password = \t\t \"password\"    >********</prop>", xml);
    }

    @Test
    public void testSecureXMLAttribute3() throws Exception {
        String xml = "<prop password=\"password\" foo=\"bar\" />";
        checkStAX("<prop password=\"********\" foo=\"bar\"></prop>", xml);
        checkRegexp("<prop password=\"********\" foo=\"bar\" />", xml);
    }

    @Test
    public void testSecureXMLAttribute4() throws Exception {
        String xml = "<prop what=\"baz\" foo=\"bar\" />";
        check(xml, xml);
    }

    @Test
    public void testSecureXMLStartAttribute() throws Exception {
        check("<prop name=\"passwordExample\">********</prop>", "<prop name=\"passwordExample\">password</prop>");
    }

    @Test
    public void testSecureXMLStartAttributeSpace() throws Exception {
        String xml = "<prop name=\"passwordExample\" >password</prop>";
        checkStAX("<prop name=\"passwordExample\">********</prop>", xml);
        checkRegexp("<prop name=\"passwordExample\" >********</prop>", xml);
    }

    @Test
    public void testSecureXMLStartAttribute2() throws Exception {
        check("<prop passwordExample=\"********\">********</prop>", "<prop passwordExample=\"password\">foo</prop>");
    }

    @Test
    public void testSecureXMLStartAttribute3() throws Exception {
        String xml = "<prop passwordExample=\"password\" foo=\"bar\">baz</prop>";
        checkStAX("<prop foo=\"bar\" passwordExample=\"********\">********</prop>", xml);
        checkRegexp("<prop passwordExample=\"********\" foo=\"bar\">baz</prop>", xml);
    }

    @Test
    public void testSecureXMLEndAttribute() throws Exception {
        check("<prop name=\"passwordExample\">********</prop>", "<prop name=\"passwordExample\">password</prop>");
    }

    @Test
    public void testSecureXMLEndAttributeSpace() throws Exception {
        String xml = "<prop name=\"passwordExample\" >password</prop>";
        checkStAX("<prop name=\"passwordExample\">********</prop>", xml);
        checkRegexp("<prop name=\"passwordExample\" >********</prop>", xml);
    }

    @Test
    public void testSecureXMLEndAttribute2() throws Exception {
        check("<prop passwordExample=\"********\">********</prop>", "<prop passwordExample=\"password\">foo</prop>");
    }

    @Test
    public void testSecureXMLEndAttribute3() throws Exception {
        String xml = "<prop passwordExample=\"password\" foo=\"bar\" />";
        checkStAX("<prop foo=\"bar\" passwordExample=\"********\"></prop>", xml);
        checkRegexp("<prop passwordExample=\"********\" foo=\"bar\" />", xml);
    }

    @Test
    public void testSecureXMLWhitelistedAttribute() throws Exception {
        check("<prop name=\"passwordField\">password</prop>", "<prop name=\"passwordField\">password</prop>");
    }

    @Test
    public void testSecureXMLWhitelistedAttributeSpace() throws Exception {
        String xml = "<prop name=\"passwordField\" >password</prop>";
        checkStAX("<prop name=\"passwordField\">password</prop>", xml);
        checkRegexp("<prop name=\"passwordField\" >password</prop>", xml);
    }

    @Test
    public void testSecureXMLWhitelistedAttribute2() throws Exception {
        check("<prop passwordField=\"password\">********</prop>", "<prop passwordField=\"password\">foo</prop>");
    }

    @Test
    public void testSecureXMLWhitelistedAttribute3() throws Exception {
        String xml = "<prop passwordField=\"password\" foo=\"bar\"/>";
        checkStAX("<prop foo=\"********\" passwordField=\"password\"></prop>", xml);
        checkRegexp("<prop passwordField=\"password\" foo=\"bar\"/>", xml);
    }

    @Test
    public void testClosedTagsUseCases() throws Exception {
        String xml = "<root>\n" //
                + "<password1>p1</password1>\n" //
                + "<password2>\n" //
                + "p1</password2>\n" //
                + "<password3>\n" //
                + "<!-- password3 -->p1</password3>\n" //
                + "<password4 />\n" //
                + "<prop5 name=\"password\">p1</prop5>\n" //
                + "<prop6 name='password'>p1</prop6>\n" //
                + "<prop7 name=\"password\" value=\"p1\" />\n" //
                + "<prop8 name='password' value='p1' />\n" //
                + "</root>";
        checkStAX("<root>\n" //
                + "<password1>********</password1>\n" //
                + "<password2>********</password2>\n" //
                + "<password3>\n" //
                + "  <!-- password3 -->\n" //
                + "  ********\n" //
                + "</password3>\n" //
                + "<password4></password4>\n" //
                + "<prop5 name=\"password\">********</prop5>\n" //
                + "<prop6 name=\"password\">********</prop6>\n" //
                + "<prop7 name=\"password\" value=\"********\"></prop7>\n" //
                + "<prop8 name=\"password\" value=\"********\"></prop8>\n" //
                + "</root>", xml);
        checkRegexp("<root>\n" //
                + "<password1>********</password1>\n" //
                + "<password2>********</password2>\n" //
                + "<password3>\n" //
                + "<!-- password3 -->p1</password3>\n" //
                + "<password4 />\n" //
                + "<prop5 name=\"password\">********</prop5>\n" //
                + "<prop6 name='password'>********</prop6>\n" //
                + "<prop7 name=\"password\" value=\"p1\" />\n" //
                + "<prop8 name='password' value='p1' />\n" //
                + "</root>", xml);
    }

    @Test
    public void testSecureXML() throws Exception {
        assertEquals(SecureXMLHelper.DEFAULT_KEYWORDS, SecureXMLHelper.getKeywords());
        assertEquals(SecureXMLHelper.DEFAULT_WHITELISTED_KEYWORDS, SecureXMLHelper.getWhitelistedKeywords());
        String xml = "<root>\n" //
                + "<password>p1</password>\n" //
                + " <myPassword>p2</myPassword>\n" //
                + " <yo password=\"p3\" other=\"bla\" />\n" //
                + " <yo otherPassword=\"p4\" other=\"bla\" />\n" //
                + " <prop name=\"password\">p5</prop>\n" //
                + " <prop name=\"realPassword\">p6</prop>\n" //
                + " <prop name=\"realPassword\" >p6</prop>\n" //
                + " <prop name=\"passwordNotWhitelisted\">p7</prop>\n" //
                + " <prop name=\"passwordField\">ok</prop>\n" //
                + " <secret>${nuxeo.jwt.secret}</secret>\n" //
                + " <secretKey>${nuxeo.aws.secretKey}</secretKey>\n" //
                + " <option name=\"apiKey\">${metrics.datadog.apiKey}</option>\n" //
                + " <passwordField>password</passwordField>\n" //
                + " <passwordHashAlgorithm>SSHA</passwordHashAlgorithm>\n" //
                + "</root>";
        String expected = "<root>\n" //
                + "<password>********</password>\n" //
                + " <myPassword>********</myPassword>\n" //
                + " <yo password=\"********\" other=\"bla\"></yo>\n" //
                + " <yo other=\"bla\" otherPassword=\"********\"></yo>\n" //
                + " <prop name=\"password\">********</prop>\n" //
                + " <prop name=\"realPassword\">********</prop>\n" //
                + " <prop name=\"realPassword\">********</prop>\n" //
                + " <prop name=\"passwordNotWhitelisted\">********</prop>\n" //
                + " <prop name=\"passwordField\">ok</prop>\n" //
                + " <secret>********</secret>\n" //
                + " <secretKey>********</secretKey>\n" //
                + " <option name=\"apiKey\">********</option>\n" //
                + " <passwordField>password</passwordField>\n" //
                + " <passwordHashAlgorithm>SSHA</passwordHashAlgorithm>\n" //
                + "</root>";
        checkStAX(expected, xml);
    }

    @Test
    public void testSecureXMLInvalid() throws Exception {
        String xml = "foo <password>p1</password>\n" //
                + " <myPassword>p2</myPassword>\n" //
                + " <myPassword >p2</myPassword>\n" //
                + " <yo password=\"p3\" other=\"bla\">\n" // not closed
                + " <yo otherPassword=\"p4\" other=\"bla\">content</yo>\n" //
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
                + " <yo otherPassword=\"********\" other=\"bla\">content</yo>\n" //
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
        checkRegexp(expected, xml);
    }

    @Test
    @Deploy("org.nuxeo.apidoc.repo.test:apidoc-secure-xml-test-contrib.xml")
    public void testSecureXMLOverride() throws Exception {
        assertEquals(List.of("secret", "apiKey"), SecureXMLHelper.getKeywords());
        assertEquals(List.of("apiKeyPublic"), SecureXMLHelper.getWhitelistedKeywords());
        String xml = "<root>\n" //
                + " <password>p1</password>\n" //
                + " <myPassword>p2</myPassword>\n" //
                + " <secret>${nuxeo.jwt.secret}</secret>\n" //
                + " <secretKey>${nuxeo.aws.secretKey}</secretKey>\n" //
                + " <option name=\"apiKey\">${metrics.datadog.apiKey}</option>\n" //
                + " <apiKeyPublic>password</apiKeyPublic>\n" //
                + "</root>";
        String expected = "<root>\n" //
                + " <password>p1</password>\n" //
                + " <myPassword>p2</myPassword>\n" //
                + " <secret>********</secret>\n" //
                + " <secretKey>********</secretKey>\n" //
                + " <option name=\"apiKey\">********</option>\n" //
                + " <apiKeyPublic>password</apiKeyPublic>\n" //
                + "</root>";
        check(expected, xml);
    }

}
