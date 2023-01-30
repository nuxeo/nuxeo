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
        return out.toString();
    }

    protected void checkEquals(String expected, String actual) {
        JSONAssert.assertEquals(expected, actual, true);
    }

    @Test
    public void testEmptyChain() throws Exception {
        String chain = getJsonChain("empty_chain");
        String res = """
                {
                  "id" : "empty_chain",
                  "label" : "empty_chain",
                  "category" : "Chain",
                  "requires" : null,
                  "description" : null,
                  "url" : "empty_chain",
                  "signature" : [ "void", "void" ],
                  "params" : [ ],
                }""";
        checkEquals(res, chain);
    }

    @Test
    public void testChain() throws Exception {
        String chain = getJsonChain("chain");
        String res = """
                {
                  "id" : "chain",
                  "label" : "chain",
                  "category" : "Chain",
                  "requires" : null,
                  "description" : "My desc",
                  "url" : "chain",
                  "signature" : [ "document", "document", "documents", "documents" ],
                  "params" : [ ],
                }""";
        checkEquals(res, chain);
    }

    @Test
    public void testChainAlt() throws Exception {
        String chain = getJsonChain("chain_props");
        String res = """
                {
                  "id" : "chain_props",
                  "label" : "chain_props",
                  "category" : "Chain",
                  "requires" : null,
                  "description" : null,
                  "url" : "chain_props",
                  "signature" : [ "document", "document", "documents", "documents" ],
                  "params" : [ ],
                }""";
        checkEquals(res, chain);
    }

    @Test
    public void testChainWithParams() throws Exception {
        String chain = getJsonChain("chain_with_params");
        String res = """
                {
                  "id" : "chain_with_params",
                  "label" : "chain_with_params",
                  "category" : "Chain",
                  "requires" : null,
                  "description" : "This is an awesome chain!",
                  "url" : "chain_with_params",
                  "signature" : [ "document", "document", "documents", "documents" ],
                  "params" : [ {
                    "name" : "foo",
                    "description" : null,
                    "type" : "string",
                    "required" : false,
                    "widget" : null,
                    "order" : 0,
                    "values" : [ "bar" ]
                  }, {
                    "name" : "foo2",
                    "description" : "yop",
                    "type" : "boolean",
                    "required" : false,
                    "widget" : null,
                    "order" : 0,
                    "values" : [ ]
                  } ],
                }""";
        checkEquals(res, chain);
    }

    @Test
    public void testChainSample() throws Exception {
        String chain = getJsonChain("chain_complex");
        String res = """
                {
                  "id" : "chain_complex",
                  "label" : "chain_complex",
                  "category" : "Chain",
                  "requires" : null,
                  "description" : null,
                  "url" : "chain_complex",
                  "signature" : [ "document", "document", "documents", "documents" ],
                  "params" : [ ],
                }""";
        checkEquals(res, chain);
    }

    @Test
    public void testOperationTypeSetProperty() throws Exception {
        String chain = getJsonChain("Document.SetProperty");
        String res = """
                {
                  "id" : "Document.SetProperty",
                  "label" : "Update Property",
                  "category" : "Document",
                  "requires" : null,
                  "description" : "Set a single property value on the input document. The property is specified using its xpath. Save parameter automatically saves the document in the database. It has to be turned off when this operation is used in the context of the empty document created, about to create, before document modification, document modified events. Returns the modified document.",
                  "url" : "Document.SetProperty",
                  "signature" : [ "document", "document", "documents", "documents" ],
                  "params" : [ {
                    "name" : "xpath",
                    "description" : "",
                    "type" : "string",
                    "required" : true,
                    "widget" : null,
                    "order" : 0,
                    "values" : [ ]
                  }, {
                    "name" : "save",
                    "description" : "",
                    "type" : "boolean",
                    "required" : false,
                    "widget" : null,
                    "order" : 0,
                    "values" : [ "true" ]
                  }, {
                    "name" : "value",
                    "description" : "",
                    "type" : "serializable",
                    "required" : false,
                    "widget" : null,
                    "order" : 0,
                    "values" : [ ]
                  } ],
                  "widgets" : [ {
                    "name" : "xpath",
                    "type" : "codearea",
                    "labels" : {
                      "any" : "XPath"
                    },
                    "translated" : true,
                    "fields" : [ {
                      "fieldName" : "xpath",
                      "propertyName" : "xpath"
                    } ],
                    "properties" : {
                      "any" : {
                        "height" : "100%",
                        "language" : "xpath",
                        "width" : "100%"
                      }
                    }
                  } ]
                }""";
        checkEquals(res, chain);
    }

    /**
     * @since 5.9.5
     */
    @Test
    public void testOperationWithWidgetDescriptor() throws Exception {
        final String chain = getJsonChain("Document.Query");
        String res = """
                {
                  "id" : "Document.Query",
                  "label" : "Query",
                  "category" : "Fetch",
                  "requires" : null,
                  "description" : "Perform a query on the repository. The query result will become the input for the next operation.",
                  "url" : "Document.Query",
                  "signature" : [ "void",
                 "documents" ],
                  "params" : [ {
                    "name" : "query",
                    "description" : "",
                    "type" : "string",
                    "required" : true,
                    "widget" : null,
                    "order" : 0,
                    "values" : [ ]
                  }, {
                    "name" : "language",
                    "description" : "",
                    "type" : "string",
                    "required" : false,
                    "widget" : "Option",
                    "order" : 0,
                    "values" : [ "NXQL",
                 "CMISQL" ]
                  } ],
                  "widgets" : [ {
                    "name" : "query",
                    "type" : "codearea",
                    "labels" : {
                      "any" : "Query"
                    },
                    "translated" : true,
                    "fields" : [ {
                      "fieldName" : "query",
                      "propertyName" : "query"
                    } ],
                    "properties" : {
                      "any" : {
                        "height" : "100%",
                        "language" : "nxql",
                        "width" : "100%"
                      }
                    }
                  } ]
                }""";
        checkEquals(res, chain);
    }

}
