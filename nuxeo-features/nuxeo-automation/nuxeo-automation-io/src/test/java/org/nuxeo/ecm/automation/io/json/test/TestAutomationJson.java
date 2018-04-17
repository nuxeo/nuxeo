/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.automation.io.json.test;

import java.io.ByteArrayOutputStream;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationType;
import org.nuxeo.ecm.automation.jaxrs.io.JsonWriter;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.ecm.webengine.test.WebEngineFeatureCore;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.skyscreamer.jsonassert.JSONAssert;

/**
 * @since 5.9.4
 */
@RunWith(FeaturesRunner.class)
@Features({ PlatformFeature.class, WebEngineFeatureCore.class })
@Deploy("org.nuxeo.ecm.automation.core")
@Deploy("org.nuxeo.ecm.automation.io")
@Deploy("org.nuxeo.ecm.platform.forms.layout.export")
@Deploy("org.nuxeo.ecm.automation.io:test-chains.xml")
@Deploy("org.nuxeo.ecm.automation.io:operations-contrib-test.xml")
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
        JSONAssert.assertEquals(expected, actual, true);
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
        res.append("  \"params\" : [ ],\n");
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
        res.append("  \"params\" : [ ],\n");
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
        res.append("  \"params\" : [ ],\n");
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
        res.append("  } ],\n");
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
        res.append("  \"params\" : [ ],\n");
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
        res.append("  } ],\n");
        res.append("  \"widgets\" : [ {\n");
        res.append("    \"name\" : \"xpath\",\n");
        res.append("    \"type\" : \"codearea\",\n");
        res.append("    \"labels\" : {\n");
        res.append("      \"any\" : \"XPath\"\n");
        res.append("    },\n");
        res.append("    \"translated\" : true,\n");
        res.append("    \"handlingLabels\" : false,\n");
        res.append("    \"fields\" : [ {\n");
        res.append("      \"fieldName\" : \"xpath\",\n");
        res.append("      \"propertyName\" : \"xpath\"\n");
        res.append("    } ],\n");
        res.append("    \"properties\" : {\n");
        res.append("      \"any\" : {\n");
        res.append("        \"height\" : \"100%\",\n");
        res.append("        \"language\" : \"xpath\",\n");
        res.append("        \"width\" : \"100%\"\n");
        res.append("      }\n");
        res.append("    }\n");
        res.append("  } ]\n");
        res.append("}");
        res.append("}");
        checkEquals(res.toString(), chain);
    }

    /**
     * @since 5.9.5
     */
    @Test
    public void testOperationWithWidgetDescriptor() throws Exception {
        final String chain = getJsonChain("Document.Query");
        StringBuilder res = new StringBuilder();
        res.append("{\n");
        res.append("  \"id\" : \"Document.Query\",\n");
        res.append("  \"label\" : \"Query\",\n");
        res.append("  \"category\" : \"Fetch\",\n");
        res.append("  \"requires\" : null,\n");
        res.append("  \"description\" : \"Perform a query on the repository. The query result will become the input for the next operation.\",\n");
        res.append("  \"url\" : \"Document.Query\",\n");
        res.append("  \"signature\" : [ \"void\",\n \"documents\" ],\n");
        res.append("  \"params\" : [ {\n");
        res.append("    \"name\" : \"query\",\n");
        res.append("    \"description\" : \"\",\n");
        res.append("    \"type\" : \"string\",\n");
        res.append("    \"required\" : true,\n");
        res.append("    \"widget\" : null,\n");
        res.append("    \"order\" : 0,\n");
        res.append("    \"values\" : [ ]\n");
        res.append("  }, {\n");
        res.append("    \"name\" : \"language\",\n");
        res.append("    \"description\" : \"\",\n");
        res.append("    \"type\" : \"string\",\n");
        res.append("    \"required\" : false,\n");
        res.append("    \"widget\" : \"Option\",\n");
        res.append("    \"order\" : 0,\n");
        res.append("    \"values\" : [ \"NXQL\",\n \"CMISQL\" ]\n");
        res.append("  } ],\n");
        res.append("  \"widgets\" : [ {\n");
        res.append("    \"name\" : \"query\",\n");
        res.append("    \"type\" : \"codearea\",\n");
        res.append("    \"labels\" : {\n");
        res.append("      \"any\" : \"Query\"\n");
        res.append("    },\n");
        res.append("    \"translated\" : true,\n");
        res.append("    \"handlingLabels\" : false,\n");
        res.append("    \"fields\" : [ {\n");
        res.append("      \"fieldName\" : \"query\",\n");
        res.append("      \"propertyName\" : \"query\"\n");
        res.append("    } ],\n");
        res.append("    \"properties\" : {\n");
        res.append("      \"any\" : {\n");
        res.append("        \"height\" : \"100%\",\n");
        res.append("        \"language\" : \"nxql\",\n");
        res.append("        \"width\" : \"100%\"\n");
        res.append("      }\n");
        res.append("    }\n");
        res.append("  } ]\n");
        res.append("}");
        checkEquals(res.toString(), chain);
    }

}
