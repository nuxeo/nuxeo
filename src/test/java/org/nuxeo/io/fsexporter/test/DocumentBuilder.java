/*
 * (C) Copyright 2018 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.io.fsexporter.test;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;

/**
 *
 *
 * @since 10.3
 */
public class DocumentBuilder {
    private String docType;

    private String path;

    private String name;

    private String title;

    private Blob content;

    private List<Blob> additonalContent = new ArrayList<>();

    public DocumentBuilder(String docType) {
        this.docType = docType;
    }

    public static DocumentBuilder folder() {
        return new DocumentBuilder("Folder");
    }

    public static DocumentBuilder file() {
        return new DocumentBuilder("File");
    }

    public DocumentBuilder at(String path) {
        this.path = path;
        return this;
    }

    public DocumentBuilder withName(String name) {
        this.name = name;
        return this;
    }

    public DocumentBuilder withTitle(String title) {
        this.title = title;
        return this;
    }

    public DocumentModel create(CoreSession session) {

        DocumentModel doc = session.createDocumentModel(path, name, docType);
        if (StringUtils.isNotBlank(title)) {
            doc.setPropertyValue("dc:title", title);
        }
        if ("File".equals(docType) && content != null) {
            doc.setPropertyValue("file:content", (Serializable) content);
        }

        if ("File".equals(docType) && !additonalContent.isEmpty()) {
            ArrayList<Map<String, Serializable>> blobs = new ArrayList<>();

            additonalContent.forEach(b -> {
                Map<String, Serializable> mapBlob = new HashMap<>();
                mapBlob.put("file", (Serializable) b);
                blobs.add(mapBlob);
            });

            doc.setPropertyValue("files:files", blobs);
        }

        return session.createDocument(doc);
    }

    public DocumentBuilder withContent(String filename, String content) {
        this.content = new StringBlob(content, "text/plain", "UTF-8", filename);
        return this;
    }

    public DocumentBuilder withAdditionalContent(String filename, String content) {
        this.additonalContent.add(new StringBlob(content, "text/plain", "UTF-8", filename));
        return this;
    }
}
