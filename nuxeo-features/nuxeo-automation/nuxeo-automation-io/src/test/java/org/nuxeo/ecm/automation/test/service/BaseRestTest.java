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
 *     Nelson Silva <nelson.silva@inevo.pt>
 */
package org.nuxeo.ecm.automation.test.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import javax.inject.Inject;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.io.registry.MarshallerHelper;
import org.nuxeo.ecm.core.io.registry.MarshallingConstants;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext.CtxBuilder;
import org.nuxeo.ecm.core.io.registry.context.RenderingContextImpl.RenderingContextBuilder;
import org.nuxeo.runtime.test.runner.Deploy;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @since 6.0
 */
@Deploy("org.nuxeo.ecm.core.io")
public class BaseRestTest {

    @Inject
    protected CoreSession session;

    @Inject
    JsonFactory factory;

    protected void assertEqualsJson(String expected, String actual) throws Exception {
        JSONAssert.assertEquals(expected, actual, JSONCompareMode.NON_EXTENSIBLE);
    }

    /**
     * Parses a JSON string into a JsonNode
     *
     * @param json
     * @return
     * @throws java.io.IOException
     */
    protected JsonNode parseJson(String json) throws JsonProcessingException, IOException {
        ObjectMapper m = new ObjectMapper();
        return m.readTree(json);
    }

    protected JsonNode parseJson(ByteArrayOutputStream out) throws JsonProcessingException, IOException {
        return parseJson(out.toString());
    }

    /**
     * Returns the JSON representation of the document with all schemas. A category may be passed to have impact on the
     * Content Enrichers.
     */
    protected String getFullDocumentAsJson(DocumentModel doc, String category) throws Exception {
        RenderingContextBuilder builder = CtxBuilder.builder();
        builder.properties(MarshallingConstants.WILDCARD_VALUE);
        builder.enrichDoc(category == null ? "children" : category);
        builder.fetch("document", "versionLabel");
        return MarshallerHelper.objectToJson(doc, builder.get());
    }

    /**
     * Returns the JSON representation of the document. A category may be passed to have impact on the Content Enrichers
     */
    protected String getDocumentAsJson(DocumentModel doc, String category) throws Exception {
        RenderingContext ctx = CtxBuilder.enrichDoc(category == null ? "children" : category).get();
        return MarshallerHelper.objectToJson(doc, ctx);
    }

    /**
     * Returns the JSON representation of these docs. A category may be passed to have impact on the Content Enrichers
     */
    protected String getDocumentsAsJson(List<DocumentModel> docs, String category) throws Exception {
        RenderingContext ctx = CtxBuilder.enrichDoc(category == null ? "children" : category).get();
        return MarshallerHelper.listToJson(DocumentModel.class, docs, ctx);
    }

    /**
     * Returns the JSON representation of the document.
     */
    protected String getDocumentAsJson(DocumentModel doc) throws Exception {
        return getDocumentAsJson(doc, null);
    }

    protected JsonGenerator getJsonGenerator(OutputStream out) throws IOException {
        return factory.createJsonGenerator(out);
    }

}
