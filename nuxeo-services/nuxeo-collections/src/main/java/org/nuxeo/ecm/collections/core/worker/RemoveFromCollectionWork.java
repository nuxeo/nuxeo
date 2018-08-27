/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     <a href="mailto:grenard@nuxeo.com">Guillaume</a>
 */
package org.nuxeo.ecm.collections.core.worker;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.collections.api.CollectionManager;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.work.AbstractWork;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 7.3
 */
public class RemoveFromCollectionWork extends AbstractWork {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(RemoveFromCollectionWork.class);

    public static final String CATEGORY = "removeFromCollection";

    protected static final String TITLE = "Remove From Collection Work";

    protected List<String> collectionMemberIds;

    protected String collectionId;

    public RemoveFromCollectionWork(final String repoName, String collectionId, List<String> collectionMemberIds,
            final int offset) {
        super(CATEGORY + ":" + repoName + ":" + collectionId + ":" + offset);
        repositoryName = repoName;
        this.collectionId = collectionId;
        this.collectionMemberIds = new ArrayList<>(collectionMemberIds);
    }

    @Override
    public String getCategory() {
        return CATEGORY;
    }

    @Override
    public String getTitle() {
        return TITLE;
    }

    @Override
    public void work() {
        setStatus("Removing");
        if (collectionMemberIds != null) {
            setProgress(new Progress(0, collectionMemberIds.size()));
            if (session == null) {
                openSystemSession();
            }
            final CollectionManager collectionManager = Framework.getService(CollectionManager.class);
            for (int i = 0; i < collectionMemberIds.size(); i++) {
                log.trace(String.format("Worker %s, deleting from Collection %s, processing CollectionMember %s",
                        getId(), collectionId, collectionMemberIds.get(i)));
                if (collectionMemberIds.get(i) != null) {
                    final DocumentRef collectionMemberRef = new IdRef(collectionMemberIds.get(i));
                    if (session.exists(collectionMemberRef)) {
                        final DocumentModel collectionMember = session.getDocument(
                                new IdRef(collectionMemberIds.get(i)));
                        if (collectionManager.isCollectable(collectionMember)) {
                            collectionManager.doRemoveFromCollection(collectionMember, collectionId, session);
                        }
                    }
                }
                setProgress(new Progress(i + 1, collectionMemberIds.size()));
            }
        }
        setStatus("Done");
    }

}
