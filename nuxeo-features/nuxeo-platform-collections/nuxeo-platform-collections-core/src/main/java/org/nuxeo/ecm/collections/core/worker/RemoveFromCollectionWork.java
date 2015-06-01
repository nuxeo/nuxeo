/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     <a href="mailto:grenard@nuxeo.com">Guillaume</a>
 */
package org.nuxeo.ecm.collections.core.worker;

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
        this.repositoryName = repoName;
        this.collectionId = collectionId;
        this.collectionMemberIds = collectionMemberIds;
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
                initSession();
            }
            final CollectionManager collectionManager = Framework.getLocalService(CollectionManager.class);
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
