/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     mcedica@nuxeo.com
 */
package org.nuxeo.elasticsearch.test;

import java.io.IOException;

import javax.ws.rs.Produces;
import javax.ws.rs.ext.Provider;

import org.nuxeo.elasticsearch.io.JsonESDocumentWriter;
import org.nuxeo.ecm.core.api.DocumentModel;

import com.fasterxml.jackson.core.JsonGenerator;

/**
 * Custom writer to index the content of a note as a raw json
 */
public class CustomJsonESDocumentWriter extends JsonESDocumentWriter {

    @Override
    protected void writeSchemas(JsonGenerator jg, DocumentModel doc, String[] schemas) throws IOException {
        if (schemas == null || (schemas.length == 1 && "*".equals(schemas[0]))) {
            schemas = doc.getSchemas();
        }
        for (String schema : schemas) {
            if ("note".equals(schema)) {
                // just index the clob as raw
                jg.writeFieldName("dynamic");
                jg.writeRawValue((String) doc.getPropertyValue("note:note"));
            } else {
                writeProperties(jg, doc, schema, null);
            }
        }
    }

}
