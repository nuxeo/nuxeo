/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.automation.io.json.test;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationType;
import org.nuxeo.ecm.automation.jaxrs.io.JsonWriter;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

import com.google.inject.Inject;

/**
 * @since 5.9.4
 */
@RunWith(FeaturesRunner.class)
@Features(PlatformFeature.class)
@Deploy({ "org.nuxeo.ecm.automation.core", "org.nuxeo.ecm.automation.io" })
@LocalDeploy("org.nuxeo.ecm.automation.io:test-chains.xml")
public class TestAutomationJson {

    @Inject
    AutomationService service;

    protected String getJsonChain(String chainId) throws Exception {
        OperationType op = service.getOperation(chainId);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        JsonWriter.writeOperation(out, op.getDocumentation(), true);
        return new String(out.toByteArray());
    }

    protected void checkEquals(String expected, String actual) throws Exception {
        // System.err.println(actual);
        // remove lines when comparing to avoid failure under windows.
        assertEquals(expected.replaceAll("\r?\n", ""),
                actual.replaceAll("\r?\n", ""));
    }

    @Test
    public void testEmptyChain() throws Exception {
        String chain = getJsonChain("empty_chain");
        StringBuilder res = new StringBuilder();
        res.append("{\n");
        res.append("  \"id\" : \"empty_chain\",\n");
        res.append("  \"label\" : \"empty_chain\",\n");
        res.append("  \"category\" : \"Chain\",\n");
        res.append("  \"requires\" : null,\n");
        res.append("  \"description\" : null,\n");
        res.append("  \"url\" : \"empty_chain\",\n");
        res.append("  \"signature\" : [ \"void\", \"void\" ],\n");
        res.append("  \"params\" : [ ]\n");
        res.append("}");
        checkEquals(res.toString(), chain);
    }

    @Test
    public void testChain() throws Exception {
        String chain = getJsonChain("chain");
        StringBuilder res = new StringBuilder();
        res.append("{\n");
        res.append("  \"id\" : \"chain\",\n");
        res.append("  \"label\" : \"chain\",\n");
        res.append("  \"category\" : \"Chain\",\n");
        res.append("  \"requires\" : null,\n");
        res.append("  \"description\" : \"My desc\",\n");
        res.append("  \"url\" : \"chain\",\n");
        res.append("  \"signature\" : [ \"document\", \"document\", \"documents\", \"documents\" ],\n");
        res.append("  \"params\" : [ ]\n");
        res.append("}");
        checkEquals(res.toString(), chain);
    }

    @Test
    public void testChainAlt() throws Exception {
        String chain = getJsonChain("chain_props");
        StringBuilder res = new StringBuilder();
        res.append("{\n");
        res.append("  \"id\" : \"chain_props\",\n");
        res.append("  \"label\" : \"chain_props\",\n");
        res.append("  \"category\" : \"Chain\",\n");
        res.append("  \"requires\" : null,\n");
        res.append("  \"description\" : null,\n");
        res.append("  \"url\" : \"chain_props\",\n");
        res.append("  \"signature\" : [ \"document\", \"document\", \"documents\", \"documents\" ],\n");
        res.append("  \"params\" : [ ]\n");
        res.append("}");
        checkEquals(res.toString(), chain);
    }

    @Test
    public void testChainWithParams() throws Exception {
        String chain = getJsonChain("chain_with_params");
        StringBuilder res = new StringBuilder();
        res.append("{\n");
        res.append("  \"id\" : \"chain_with_params\",\n");
        res.append("  \"label\" : \"chain_with_params\",\n");
        res.append("  \"category\" : \"Chain\",\n");
        res.append("  \"requires\" : null,\n");
        res.append("  \"description\" : \"This is an awesome chain!\",\n");
        res.append("  \"url\" : \"chain_with_params\",\n");
        res.append("  \"signature\" : [ \"document\", \"document\", \"documents\", \"documents\" ],\n");
        res.append("  \"params\" : [ {\n");
        res.append("    \"name\" : \"foo\",\n");
        res.append("    \"description\" : null,\n");
        res.append("    \"type\" : \"string\",\n");
        res.append("    \"required\" : false,\n");
        res.append("    \"widget\" : null,\n");
        res.append("    \"order\" : 0,\n");
        res.append("    \"values\" : [ \"bar\" ]\n");
        res.append("  }, {\n");
        res.append("    \"name\" : \"foo2\",\n");
        res.append("    \"description\" : \"yop\",\n");
        res.append("    \"type\" : \"boolean\",\n");
        res.append("    \"required\" : false,\n");
        res.append("    \"widget\" : null,\n");
        res.append("    \"order\" : 0,\n");
        res.append("    \"values\" : [ ]\n");
        res.append("  } ]\n");
        res.append("}");
        checkEquals(res.toString(), chain);
    }

    @Test
    public void testChainSample() throws Exception {
        String chain = getJsonChain("chain_complex");
        StringBuilder res = new StringBuilder();
        res.append("{\n");
        res.append("  \"id\" : \"chain_complex\",\n");
        res.append("  \"label\" : \"chain_complex\",\n");
        res.append("  \"category\" : \"Chain\",\n");
        res.append("  \"requires\" : null,\n");
        res.append("  \"description\" : null,\n");
        res.append("  \"url\" : \"chain_complex\",\n");
        res.append("  \"signature\" : [ \"document\", \"document\", \"documents\", \"documents\" ],\n");
        res.append("  \"params\" : [ ]\n");
        res.append("}");
        checkEquals(res.toString(), chain);
    }

    @Test
    public void testOperationTypeSetProperty() throws Exception {
        String chain = getJsonChain("Document.SetProperty");
        StringBuilder res = new StringBuilder();
        res.append("{\n");
        res.append("  \"id\" : \"Document.SetProperty\",\n");
        res.append("  \"label\" : \"Update Property\",\n");
        res.append("  \"category\" : \"Document\",\n");
        res.append("  \"requires\" : null,\n");
        res.append("  \"description\" : \"Set a single property value on the input document. The property is specified using its xpath. The document is automatically saved if 'save' parameter is true. If you unset the 'save' you need to save it later using Save Document operation. Return the modified document.\",\n");
        res.append("  \"url\" : \"Document.SetProperty\",\n");
        res.append("  \"signature\" : [ \"document\", \"document\", \"documents\", \"documents\" ],\n");
        res.append("  \"params\" : [ {\n");
        res.append("    \"name\" : \"xpath\",\n");
        res.append("    \"description\" : \"\",\n");
        res.append("    \"type\" : \"string\",\n");
        res.append("    \"required\" : true,\n");
        res.append("    \"widget\" : null,\n");
        res.append("    \"order\" : 0,\n");
        res.append("    \"values\" : [ ]\n");
        res.append("  }, {\n");
        res.append("    \"name\" : \"save\",\n");
        res.append("    \"description\" : \"\",\n");
        res.append("    \"type\" : \"boolean\",\n");
        res.append("    \"required\" : false,\n");
        res.append("    \"widget\" : null,\n");
        res.append("    \"order\" : 0,\n");
        res.append("    \"values\" : [ \"true\" ]\n");
        res.append("  }, {\n");
        res.append("    \"name\" : \"value\",\n");
        res.append("    \"description\" : \"\",\n");
        res.append("    \"type\" : \"serializable\",\n");
        res.append("    \"required\" : false,\n");
        res.append("    \"widget\" : null,\n");
        res.append("    \"order\" : 0,\n");
        res.append("    \"values\" : [ ]\n");
        res.append("  } ]\n");
        res.append("}");
        checkEquals(res.toString(), chain);
    }

}
