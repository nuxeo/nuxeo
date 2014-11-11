/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     dmetzler
 */
package org.nuxeo.ecm.restapi.test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

import org.codehaus.jackson.JsonGenerator;
import org.nuxeo.ecm.automation.io.services.JsonFactoryManager;
import org.nuxeo.ecm.automation.jaxrs.io.documents.JsonDocumentListWriter;
import org.nuxeo.ecm.automation.jaxrs.io.documents.JsonDocumentWriter;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 5.7.3
 */
public class JSONDocumentHelper {

    private static final String[] DEFAULT_SCHEMAS = new String[] {};

    public static String getDocAsJson(String[] schemas, DocumentModel doc)
            throws Exception {
        OutputStream out = new ByteArrayOutputStream();
        JsonGenerator jg = getJsonGenerator(out);
        JsonDocumentWriter.writeDocument(jg, doc, DEFAULT_SCHEMAS);
        return out.toString();

    }

    private static JsonGenerator getJsonGenerator(OutputStream out)
            throws IOException {
        JsonFactoryManager factoryProvider = Framework.getLocalService(JsonFactoryManager.class);
        return factoryProvider.getJsonFactory().createJsonGenerator(out);
    }

    public static String getDocAsJson(DocumentModel doc) throws Exception {
        return getDocAsJson(DEFAULT_SCHEMAS, doc);
    }

    public static String getDocsListAsJson(String[] schemas,
            DocumentModel... docs) throws Exception {
        OutputStream out = new ByteArrayOutputStream();
        DocumentModelList docList = new DocumentModelListImpl(
                Arrays.asList(docs));
        JsonDocumentListWriter.writeDocuments(getJsonGenerator(out), docList,
                schemas);
        return out.toString();
    }

    public static String getDocsListAsJson(DocumentModel... docs)
            throws Exception {
        return getDocsListAsJson(DEFAULT_SCHEMAS, docs);
    }

}
