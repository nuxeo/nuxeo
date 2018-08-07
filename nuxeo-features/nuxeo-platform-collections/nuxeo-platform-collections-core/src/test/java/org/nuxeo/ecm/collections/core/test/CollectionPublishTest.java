/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     vdutat
 */
package org.nuxeo.ecm.collections.core.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.collections.api.FavoritesManager;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.SortInfo;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.ecm.platform.query.nxql.CoreQueryDocumentPageProvider;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.transaction.TransactionHelper;

@RunWith(FeaturesRunner.class)
@Features(CollectionFeature.class)
@Deploy("org.nuxeo.ecm.user.center.dashboard")
public class CollectionPublishTest {

    protected static final String TEST_FILE_NAME = "testFile";

    @Inject
    FavoritesManager favoritesManager;

    @Inject
    CoreSession session;

    @Inject
    PageProviderService pps;

    @Inject
    protected EventService eventService;

    @Test
    public void addToFavoritesAndPublish() throws Exception {
        DocumentModel testWorkspace = session.createDocumentModel("/default-domain/workspaces", "testWorkspace",
                "Workspace");
        testWorkspace = session.createDocument(testWorkspace);
        DocumentModel testFile = session.createDocumentModel(testWorkspace.getPathAsString(), TEST_FILE_NAME, "File");
        testFile = session.createDocument(testFile);
        favoritesManager.addToFavorites(testFile, session);
        assertTrue(favoritesManager.isFavorite(testFile, session));

        waitForAsyncCompletion();
        List<SortInfo> sortInfos = null;
        Map<String, Serializable> props = new HashMap<String, Serializable>();
        props.put(CoreQueryDocumentPageProvider.CORE_SESSION_PROPERTY, (Serializable) session);
        DocumentModel favoritesDoc = favoritesManager.getFavorites(null, session);
        PageProvider<DocumentModel> pageProvider = (PageProvider<DocumentModel>) pps.getPageProvider("user_favorites",
                sortInfos, null, null, props, new Object[] { favoritesDoc.getId() });
        List<DocumentModel> list = pageProvider.getCurrentPage();
        assertEquals(1, list.size());

        PathRef sectionsRootRef = new PathRef("/default-domain/sections");
        assertTrue(session.exists(sectionsRootRef));
        DocumentModel sectionDoc = session.getDocument(sectionsRootRef);
        sectionDoc = session.createDocumentModel("Section");
        sectionDoc.setPathInfo(sectionDoc.getPathAsString(), "section1");
        sectionDoc = session.createDocument(sectionDoc);
        session.publishDocument(testFile, sectionDoc);
        waitForAsyncCompletion();
        pageProvider.refresh();
        list = pageProvider.getCurrentPage();
        assertEquals(1, list.size());
    }

    protected void waitForAsyncCompletion() {
        if (TransactionHelper.isTransactionActiveOrMarkedRollback()) {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();
        }
        eventService.waitForAsyncCompletion();
    }

}
