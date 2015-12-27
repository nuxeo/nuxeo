/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     dmetzler
 */
package org.nuxeo.ecm.restapi.test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

import org.codehaus.jackson.JsonGenerator;
import org.nuxeo.ecm.automation.jaxrs.io.documents.JsonDocumentListWriter;
import org.nuxeo.ecm.automation.jaxrs.io.documents.JsonDocumentWriter;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.restapi.jaxrs.io.documents.JSONDocumentModelListReader;
import org.nuxeo.ecm.webengine.JsonFactoryManager;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 5.7.3
 * @deprecated since 7.10 see {@link JSONDocumentModelListReader} - do not use it for anything else
 */
@Deprecated
public class JSONDocumentHelper {

    private static final String[] DEFAULT_SCHEMAS = new String[] {};

    public static String getDocAsJson(String[] schemas, DocumentModel doc) throws Exception {
        OutputStream out = new ByteArrayOutputStream();
        JsonGenerator jg = getJsonGenerator(out);
        JsonDocumentWriter.writeDocument(jg, doc, DEFAULT_SCHEMAS, null);
        return out.toString();

    }

    private static JsonGenerator getJsonGenerator(OutputStream out) throws IOException {
        JsonFactoryManager factoryProvider = Framework.getLocalService(JsonFactoryManager.class);
        return factoryProvider.getJsonFactory().createJsonGenerator(out);
    }

    public static String getDocAsJson(DocumentModel doc) throws Exception {
        return getDocAsJson(DEFAULT_SCHEMAS, doc);
    }

    public static String getDocsListAsJson(String[] schemas, DocumentModel... docs) throws Exception {
        OutputStream out = new ByteArrayOutputStream();
        DocumentModelList docList = new DocumentModelListImpl(Arrays.asList(docs));
        JsonDocumentListWriter.writeDocuments(getJsonGenerator(out), docList, schemas, null);
        return out.toString();
    }

    public static String getDocsListAsJson(DocumentModel... docs) throws Exception {
        return getDocsListAsJson(DEFAULT_SCHEMAS, docs);
    }

}
