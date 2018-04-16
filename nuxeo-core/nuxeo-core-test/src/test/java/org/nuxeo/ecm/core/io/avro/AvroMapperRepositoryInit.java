/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     pierre
 */
package org.nuxeo.ecm.core.io.avro;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.model.impl.ListProperty;
import org.nuxeo.ecm.core.test.annotations.RepositoryInit;

/**
 * @since 10.2
 */
public class AvroMapperRepositoryInit implements RepositoryInit {

    @Override
    public void populate(CoreSession session) {
        try {
            createDocument(session, "myComplexDocFull", true);
            createDocument(session, "myComplexDocPartial", false);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void createDocument(CoreSession session, String name, boolean full) throws IOException {
        DocumentModel doc = session.createDocumentModel("/", name, "ComplexDoc");
        // common
        doc.getProperty("common:icon").setValue("/path/to/my/icon");
        if (full) {
            doc.getProperty("common:icon-expanded").setValue("/path/to/my/icon-expanded");
        }
        // dublin
        doc.getProperty("dc:title").setValue("title");
        doc.getProperty("dc:rights").setValue("rights");
        doc.getProperty("dc:created").setValue(new Date());
        doc.getProperty("dc:modified").setValue(new Date());
        doc.getProperty("dc:subjects").setValue(new String[] { "subject1", "subject2", "subject3" });
        if (full) {
            doc.getProperty("dc:expired").setValue(new Date());
            doc.getProperty("dc:issued").setValue(new Date());
            doc.getProperty("dc:valid").setValue(new Date());
            doc.getProperty("dc:nature").setValue("nature");
            doc.getProperty("dc:source").setValue("source");
            doc.getProperty("dc:format").setValue("format");
            doc.getProperty("dc:creator").setValue("creator");
            doc.getProperty("dc:coverage").setValue("coverage");
            doc.getProperty("dc:language").setValue("language");
            doc.getProperty("dc:publisher").setValue("publisher");
            doc.getProperty("dc:description").setValue("description");
            doc.getProperty("dc:lastContributor").setValue("lastContributor");
            doc.getProperty("dc:contributors").setValue(new String[] { "contrib1", "contrib2", "contrib3" });
        }
        // complexschema
        if (full) {
            doc.getProperty("complexschema:attachedFile").setValue("name", "attachedFileName");
        }
        ListProperty vignettes = (ListProperty) doc.getProperty("complexschema:attachedFile/vignettes");
        for (String label : new String[] { "toto", "titi", "tutu", "tata" }) {
            vignettes.addValue(createVignette(label, full));
        }
        session.createDocument(doc);
    }

    private Map<String, Object> createVignette(String label, boolean full) throws IOException {
        Blob blob = full
                ? Blobs.createBlob(label.getBytes("UTF-8"), "plain/test", "UTF-8")
                : Blobs.createBlob(label);
        blob.setDigest("gest");
        Map<String, Object> vignette = new HashMap<>(4);
        if (full) {
            vignette.put("label", label);
            vignette.put("width", Integer.valueOf(new Random().nextInt(1000)));
        }
        vignette.put("height", Integer.valueOf(new Random().nextInt(1000)));
        vignette.put("content", blob);
        return vignette;
    }

}
