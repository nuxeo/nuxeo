/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Funsho David
 *
 */

package org.nuxeo.ecm.core;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.transaction.TransactionHelper;

import javax.inject.Inject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @since 9.3
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.runtime.kv")
public class TestGetOrCreateDocument {

    protected static final int NB_FILES = 500;

    protected static final int NB_THREADS = 2;

    @Inject
    protected CoreSession session;

    @Inject
    protected CoreFeature coreFeature;

    protected DocumentModelList initDocumentModels() {
        DocumentModelList docs = new DocumentModelListImpl();
        for (int i = 0; i < NB_FILES; i++) {
            docs.add(session.createDocumentModel("/", "file_" + i, "File"));
        }
        return docs;
    }

    @Test
    public void testGetOrCreateDocuments() throws Exception {

        DocumentModelList docs = initDocumentModels();

        ExecutorService executor = Executors.newFixedThreadPool(NB_THREADS);
        List<CompletableFuture<DocumentModelList>> futures = new ArrayList<>(NB_THREADS);

        for (int t = 0; t < NB_THREADS; t++) {
            CompletableFuture completableFuture = CompletableFuture.supplyAsync(
                    () -> TransactionHelper.runInTransaction(() -> {
                        try (CoreSession s = CoreInstance.openCoreSession(coreFeature.getRepositoryName())) {
                            return docs.stream().map(doc -> {
                                DocumentModel fetchedDoc = s.getOrCreateDocument(doc);
                                s.save();
                                return fetchedDoc;
                            }).collect(Collectors.toCollection(DocumentModelListImpl::new));
                        }
                    }), executor);
            futures.add(completableFuture);
        }

        List<DocumentModelList> docModelsPerThread = futures.stream()
                                                            .map(CompletableFuture::join)
                                                            .collect(Collectors.toList());

        for (DocumentModelList list : docModelsPerThread) {
            assertEquals(NB_FILES, list.size());
            for (int i = 0; i < NB_FILES; i++) {
                DocumentModel doc = list.get(i);
                assertEquals("file_" + i, doc.getName());
                assertTrue(session.exists(doc.getRef()));
            }
        }

        executor.shutdown();
    }

}
