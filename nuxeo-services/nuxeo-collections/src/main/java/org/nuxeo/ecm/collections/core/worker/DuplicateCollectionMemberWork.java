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
import org.nuxeo.ecm.collections.api.CollectionConstants;
import org.nuxeo.ecm.collections.api.CollectionManager;
import org.nuxeo.ecm.collections.core.adapter.CollectionMember;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.versioning.VersioningService;
import org.nuxeo.ecm.core.work.AbstractWork;
import org.nuxeo.ecm.platform.dublincore.listener.DublinCoreListener;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 5.9.3
 */
public class DuplicateCollectionMemberWork extends AbstractWork {

    private static final Log log = LogFactory.getLog(DuplicateCollectionMemberWork.class);

    public DuplicateCollectionMemberWork(final String repoName, final String newCollectionId,
            final List<String> collectionMemberIds, final int offset) {
        super(CATEGORY + ":" + repoName + ":" + newCollectionId + ":" + offset);
        repositoryName = repoName;
        this.newCollectionId = newCollectionId;
        this.collectionMemberIds = new ArrayList<>(collectionMemberIds);
    }

    public static final String CATEGORY = "duplicateCollectionMember";

    protected static final long serialVersionUID = 4985374651436954280L;

    protected static final String TITLE = "Duplicate CollectionMember Work";

    protected String newCollectionId;

    protected List<String> collectionMemberIds;

    @Override
    public String getCategory() {
        return CATEGORY;
    }

    public String getNewCollectionId() {
        return newCollectionId;
    }

    @Override
    public String getTitle() {
        return TITLE;
    }

    public void setNewCollectionId(String newCollectionId) {
        this.newCollectionId = newCollectionId;
    }

    @Override
    public void work() {
        setStatus("Duplicating");
        if (collectionMemberIds != null) {
            CollectionManager collectionManager = Framework.getService(CollectionManager.class);
            setProgress(new Progress(0, collectionMemberIds.size()));
            openSystemSession();
            for (int i = 0; i < collectionMemberIds.size(); i++) {
                log.trace(String.format("Worker %s, populating Collection %s, processing CollectionMember %s", getId(),
                        newCollectionId, collectionMemberIds.get(i)));
                if (collectionMemberIds.get(i) != null) {
                    DocumentModel collectionMember = session.getDocument(new IdRef(collectionMemberIds.get(i)));
                    if (collectionManager.isCollectable(collectionMember)) {

                        // We want to disable the following listener on a
                        // collection member when it is added to a collection
                        collectionMember.putContextData(DublinCoreListener.DISABLE_DUBLINCORE_LISTENER, true);
                        collectionMember.putContextData(CollectionConstants.DISABLE_NOTIFICATION_SERVICE, true);
                        collectionMember.putContextData(CollectionConstants.DISABLE_AUDIT_LOGGER, true);
                        collectionMember.putContextData(VersioningService.DISABLE_AUTO_CHECKOUT, true);

                        CollectionMember collectionMemberAdapter = collectionMember.getAdapter(CollectionMember.class);
                        collectionMemberAdapter.addToCollection(newCollectionId);
                        session.saveDocument(collectionMember);
                    }
                }
                setProgress(new Progress(i, collectionMemberIds.size()));
            }
        }
        setStatus("Done");
    }

}
