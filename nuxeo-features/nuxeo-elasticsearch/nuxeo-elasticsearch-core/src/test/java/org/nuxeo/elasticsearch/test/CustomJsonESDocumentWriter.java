/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     mcedica@nuxeo.com
 */
package org.nuxeo.elasticsearch.test;

import java.io.IOException;

import javax.ws.rs.Produces;
import javax.ws.rs.ext.Provider;

import org.codehaus.jackson.JsonGenerator;
import org.nuxeo.ecm.automation.jaxrs.io.documents.JsonESDocumentWriter;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * Custom writer to index the content of a note as a raw json
 */
@Provider
@Produces({ JsonESDocumentWriter.MIME_TYPE })
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
