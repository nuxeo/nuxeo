/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thierry Delprat
 */
package org.nuxeo.template.xdocreport.jaxrs;

import java.io.IOException;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.template.api.adapters.TemplateSourceDocument;

import fr.opensagres.xdocreport.remoting.resources.domain.LargeBinaryData;

/**
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 */
public class BinaryDataWrapper {

    public static LargeBinaryData wrap(Blob blob) throws IOException {

        LargeBinaryData data = new LargeBinaryData();
        data.setContent(blob.getStream());
        data.setFileName(blob.getFilename());
        data.setMimeType(blob.getMimeType());
        if (blob.getLength() > 0) {
            data.setLength(blob.getLength());
        }
        return data;
    }

    public static LargeBinaryData wrap(TemplateSourceDocument template) throws IOException {
        Blob blob = template.getTemplateBlob();
        LargeBinaryData data = wrap(blob);
        data.setResourceId(template.getAdaptedDoc().getId());
        return data;
    }

    public static LargeBinaryData wrapXml(String xml, String fileName) throws IOException {
        Blob blob = Blobs.createBlob(xml, "text/xml", null, fileName);
        return wrap(blob);
    }

}
