/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Stephane Lacoin
 */
package org.nuxeo.ecm.core.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.PostCommitEventListener;
import org.nuxeo.ecm.core.event.impl.EventListenerDescriptor;
import org.nuxeo.ecm.core.storage.sql.coremodel.BinaryTextListener;
import org.nuxeo.ecm.core.test.annotations.BackendType;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfigs;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.google.inject.Inject;
import com.google.inject.name.Named;

@RunWith(FeaturesRunner.class)
@Features( { MultiRepositoriesCoreFeature.class, TransactionalFeature.class } )
@RepositoryConfigs( {
    @RepositoryConfig(type=BackendType.H2, repositoryName="repo1"),
    @RepositoryConfig(type=BackendType.H2, repositoryName="repo2")
})
public class MultiRepositoriesAreTestable {

    @Inject @Named("repo1") CoreSession repo1;
    @Inject @Named("repo2") CoreSession repo2;
    @Inject EventService eventSrv;


    public static class RepoListener implements PostCommitEventListener {

        protected static RepoListener instance;

        public RepoListener() {
           RepoListener.instance = this;
        }

        List<EventBundle> bundles = new ArrayList<EventBundle>();

        boolean seenRepo1;
        boolean seenRepo2;

        @Override
        public void handleEvent(EventBundle events) throws ClientException {
            if ("repo1".equals(events.getName())) {
                seenRepo1 = true;
            }
            if ("repo2".equals(events.getName())) {
                seenRepo2 = true;
            }
            bundles.add(events);
        }

    }

    @Before public void registerPostCommitListener() {
        eventSrv.addEventListener(new EventListenerDescriptor() {
            {
                this.name = "multi-repo-test";
                this.clazz = RepoListener.class;
                this.isAsync = Boolean.TRUE;
                this.isPostCommit = true;
            }
        });
        assertThat("post commit listener is registered", RepoListener.instance, notNullValue());
    }


    @Test public void theRepo1IsUsable() throws ClientException  {
        theRepoIsUsable(repo1);
    }

    @Test public void theRepo2IsUsable() throws ClientException  {
        theRepoIsUsable(repo2);
    }

    @Test public void theReposAreMutableInTheSameTransaction() throws ClientException {
        alterRepoWithBinaries(repo1);
        alterRepoWithBinaries(repo2);
        TransactionHelper.commitOrRollbackTransaction();
        eventSrv.waitForAsyncCompletion();
        List<EventBundle> bundles = RepoListener.instance.bundles;
        assertThat("listener has handled at least two bundles", bundles.size(), greaterThan(2));
        EventBundle alterRepo1Bundle = bundles.get(0);
        theEventsComesFromSameRepository(alterRepo1Bundle);
        theEventsContainsVcsEvents(alterRepo1Bundle);
        EventBundle alterRepo2Bundle = bundles.get(1);
        theEventsComesFromSameRepository(alterRepo2Bundle);
        theEventsContainsVcsEvents(alterRepo2Bundle);
    }

    protected void theEventsComesFromSameRepository(EventBundle bundle) {
        String name = bundle.getName();
        for (Event event:bundle) {
            assertThat(event.getContext().getRepositoryName(), is(name));
        }
    }

    protected void theEventsContainsVcsEvents(EventBundle bundle) {
        assertTrue(bundle.containsEventName(BinaryTextListener.EVENT_NAME));
    }
    protected void theRepoIsUsable(CoreSession repo) throws ClientException {
        assertThat(repo, notNullValue());
        assertThat(repo.getDocument(new PathRef("/default-domain")), notNullValue());
    }

    protected void alterRepoWithBinaries(CoreSession repo) throws ClientException {
        DocumentModel doc = repo.createDocumentModel("/default-domain/workspaces/test", "file",
                "File");
        BlobHolder holder = doc.getAdapter(BlobHolder.class);
        holder.setBlob(new StringBlob("some content"));
        doc = repo.createDocument(doc);
        repo.saveDocument(doc);
        repo.save();
    }
}
