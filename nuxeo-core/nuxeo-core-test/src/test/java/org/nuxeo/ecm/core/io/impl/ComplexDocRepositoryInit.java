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
 *     ataillefer
 */

package org.nuxeo.ecm.core.io.impl;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.test.DefaultRepositoryInit;

public class ComplexDocRepositoryInit extends DefaultRepositoryInit {

    public static final String TEST_DOC_NAME = "testDoc";

    @Override
    public void populate(CoreSession session) {

        createTestDoc(session);
    }

    /**
     * Creates the test doc.
     *
     * @param session the session
     * @return the document model
     */
    protected final DocumentModel createTestDoc(CoreSession session) {

        DocumentModel doc = session.createDocumentModel("/", TEST_DOC_NAME, "CSDoc");

        // -----------------------
        // dublincore
        // -----------------------
        doc.setProperty("dublincore", "title", "My test doc");
        doc.setProperty("dublincore", "created", "2011-12-29T11:24:25Z");
        doc.setProperty("dublincore", "creator", "Administrator");
        doc.setProperty("dublincore", "modified", "2011-12-29T11:24:25Z");
        doc.setProperty("dublincore", "lastContributor", "Administrator");
        doc.setProperty("dublincore", "contributors", new String[] { "Administrator", "Joe" });
        doc.setProperty("dublincore", "subjects", new String[] { "Art", "Architecture" });

        // -----------------------
        // file
        // -----------------------
        Blob blob = Blobs.createBlob("My blob");
        blob.setFilename("test_file.doc");
        doc.setProperty("file", "content", blob);

        // ------------------------
        // CS stuffs
        // ------------------------
        doc.setPropertyValue("cs:currentVersion", "V1");
        doc.setPropertyValue("cs:modelType", "Insurance");
        doc.setPropertyValue("cs:origin", "Internal");

        // segmentVariable
        Map<String, Serializable> segment = new HashMap<>();
        segment.put("name", "MySegment");
        segment.put("target", "SomeTarget");
        segment.put("variableType", "rawVariable");

        List<String> roles = Arrays.asList("Score", "ComparisonScore", "Decision");
        segment.put("roles", (Serializable) roles);

        doc.setPropertyValue("cs:segmentVariable", (Serializable) segment);

        doc = session.createDocument(doc);

        // set ACP
        ACP acp = doc.getACP();
        acp.addACE(ACL.LOCAL_ACL, new ACE("leela", "Read"));
        Calendar begin = new GregorianCalendar(2000, 10, 10);
        Calendar end = new GregorianCalendar(2010, 10, 10);
        acp.addACE(ACL.LOCAL_ACL, ACE.builder("fry", "Write").creator("leela").begin(begin).end(end).build());
        doc.setACP(acp, true);

        return doc;
    }

}
