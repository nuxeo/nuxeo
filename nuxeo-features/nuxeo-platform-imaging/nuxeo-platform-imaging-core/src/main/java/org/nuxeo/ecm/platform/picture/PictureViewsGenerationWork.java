/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger
 *     Antoine Taillefer<ataillefer@nuxeo.com>
 */
package org.nuxeo.ecm.platform.picture;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.core.work.AbstractWork;
import org.nuxeo.runtime.api.Framework;

/**
 * Work generating the different picture views for a Picture.
 *
 * @since 5.7
 */
public class PictureViewsGenerationWork extends AbstractWork {

    private static final long serialVersionUID = 1L;

    public static final String CATEGORY_PICTURE_GENERATION = "pictureViewsGeneration";

    public static final String PICTURE_VIEWS_GENERATION_DONE_EVENT = "pictureViewsGenerationDone";

    protected final String xpath;

    public PictureViewsGenerationWork(String repositoryName, String docId, String xpath) {
        super(repositoryName + ':' + docId + ':' + xpath + ":pictureView");
        setDocument(repositoryName, docId);
        this.xpath = xpath;
    }

    @Override
    public String getCategory() {
        return CATEGORY_PICTURE_GENERATION;
    }

    @Override
    public String getTitle() {
        return "Picture views generation";
    }

    @Override
    public int getRetryCount() {
        // we could fail to get the doc due to a concurrent delete, so allow to retry
        return 2;
    }

    @Override
    public boolean isIdempotent() {
        return false;
    }

    @Override
    public boolean isGroupJoin() {
        // This is a GroupJoin work with a trigger that can be used on the last work execution
        return true;
    }

    @Override
    public String getPartitionKey() {
        return docId;
    }

    @Override
    public void onGroupJoinCompletion() {
        firePictureViewsGenerationDoneEvent(session.getDocument(new IdRef(docId)));
    }

    @Override
    public void work() {
        setProgress(Progress.PROGRESS_INDETERMINATE);
        setStatus("Extracting");

        openSystemSession();

        PictureViewsHelper pictureViewsHelper = new PictureViewsHelper();
        pictureViewsHelper.newTransaction();
        pictureViewsHelper.computePictureViews(session, docId, xpath, this::setStatus);
    }

    /**
     * Fire a {@code PICTURE_VIEWS_GENERATION_DONE_EVENT}
     *
     * @since 5.8
     */
    protected void firePictureViewsGenerationDoneEvent(DocumentModel doc) {
        DocumentEventContext ctx = new DocumentEventContext(session, session.getPrincipal(), doc);
        Event event = ctx.newEvent(PICTURE_VIEWS_GENERATION_DONE_EVENT);
        Framework.getService(EventService.class).fireEvent(event);
    }

}
