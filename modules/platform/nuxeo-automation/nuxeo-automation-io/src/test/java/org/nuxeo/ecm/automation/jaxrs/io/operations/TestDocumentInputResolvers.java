/*
 * (C) Copyright 2023 Nuxeo SA (http://nuxeo.com/) and others.
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
 *  Contributors:
 *     Antoine Taillefer
 */
package org.nuxeo.ecm.automation.jaxrs.io.operations;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.core.impl.adapters.helper.AbsoluteDocumentRef;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.DocumentRefList;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;
import org.nuxeo.runtime.test.runner.WithFrameworkProperty;

/**
 * Tests the document input resolvers.
 *
 * @see DocumentInputResolver
 * @see DocumentsInputResolver
 * @since 2023.5
 */
@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
@Deploy("org.nuxeo.ecm.automation.io")
public class TestDocumentInputResolvers {

    protected DocumentsInputResolver documentsInputsResolver = new DocumentsInputResolver();

    @Test
    public void testDocumentInputResolver() {
        assertPathRef(DocumentInputResolver.docRefFromString("/path"), "/path");
        // a path ref can include ":"
        assertPathRef(DocumentInputResolver.docRefFromString("/pa:th"), "/pa:th");
        assertIdRef(DocumentInputResolver.docRefFromString("id"), "id");
    }

    @Test
    @WithFrameworkProperty(name = DocumentInputResolver.BULK_DOWNLOAD_MULTI_REPOSITORIES, value = "true")
    public void testAbsoluteDocumentInputResolver() {
        testDocumentInputResolver();
        assertAbsolutePathRef(DocumentInputResolver.docRefFromString("repo:/path"), "repo", "/path");
        // a path ref can include ":"
        assertAbsolutePathRef(DocumentInputResolver.docRefFromString("repo:/pa:th"), "repo", "/pa:th");
        assertAbsoluteIdRef(DocumentInputResolver.docRefFromString("repo:id"), "repo", "id");
    }

    @Test
    public void testDocumentsInputResolver() {
        DocumentRefList list = documentsInputsResolver.getInput("/path,/pa:th,id,repo:/path,repo:/pa:th,repo:id");
        assertPathRef(list.get(0), "/path");
        assertPathRef(list.get(1), "/pa:th");
        assertIdRef(list.get(2), "id");
        // resolved as an ID ref as not starting with "/"
        assertIdRef(list.get(3), "repo:/path");
        assertIdRef(list.get(4), "repo:/pa:th");
        assertIdRef(list.get(5), "repo:id");
    }

    @Test
    @WithFrameworkProperty(name = DocumentInputResolver.BULK_DOWNLOAD_MULTI_REPOSITORIES, value = "true")
    public void testAbsoluteDocumentsInputResolver() {
        DocumentRefList list = documentsInputsResolver.getInput("/path,/pa:th,id,repo:/path,repo:/pa:th,repo:id");

        DocumentRef docRef = list.get(0);
        assertPathRef(docRef, "/path");
        docRef = list.get(1);
        assertPathRef(docRef, "/pa:th");
        docRef = list.get(2);
        assertIdRef(docRef, "id");
        docRef = list.get(3);
        assertAbsolutePathRef(docRef, "repo", "/path");
        docRef = list.get(4);
        assertAbsolutePathRef(docRef, "repo", "/pa:th");
        docRef = list.get(5);
        assertAbsoluteIdRef(docRef, "repo", "id");
    }

    protected void assertPathRef(DocumentRef documentRef, String path) {
        assertTrue(documentRef instanceof PathRef);
        assertEquals(DocumentRef.PATH, documentRef.type());
        assertEquals(path, documentRef.reference());
    }

    protected void assertIdRef(DocumentRef documentRef, String id) {
        assertTrue(documentRef instanceof IdRef);
        assertEquals(DocumentRef.ID, documentRef.type());
        assertEquals(id, documentRef.reference());
    }

    protected void assertAbsolutePathRef(DocumentRef documentRef, String repositoryName, String path) {
        assertTrue(documentRef instanceof AbsoluteDocumentRef);
        assertEquals(repositoryName, ((AbsoluteDocumentRef) documentRef).getRepositoryName());
        assertEquals(DocumentRef.PATH, documentRef.type());
        assertEquals(path, documentRef.reference());
    }

    protected void assertAbsoluteIdRef(DocumentRef documentRef, String repositoryName, String id) {
        assertTrue(documentRef instanceof AbsoluteDocumentRef);
        assertEquals(repositoryName, ((AbsoluteDocumentRef) documentRef).getRepositoryName());
        assertEquals(DocumentRef.ID, documentRef.type());
        assertEquals(id, documentRef.reference());
    }

}
