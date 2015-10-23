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
 *     Nelson Silva <nelson.silva@inevo.pt>
 */
package org.nuxeo.ecm.automation.test.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import javax.inject.Inject;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.ObjectMapper;
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
     * @throws org.codehaus.jackson.JsonProcessingException
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
