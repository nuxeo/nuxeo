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
import org.nuxeo.ecm.collections.core.adapter.CollectionMember;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.versioning.VersioningService;
import org.nuxeo.ecm.core.work.AbstractWork;
import org.nuxeo.ecm.platform.audit.service.NXAuditEventsService;
import org.nuxeo.ecm.platform.dublincore.listener.DublinCoreListener;
import org.nuxeo.ecm.platform.ec.notification.NotificationConstants;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 5.9.3
 */
public class DuplicateCollectionMemberWork extends AbstractWork {

    private static final Log log = LogFactory.getLog(DuplicateCollectionMemberWork.class);

    public DuplicateCollectionMemberWork(final String repoName,
            final String newCollectionId, final List<String> collectionMemberIds, final int offset) {
        super(repoName + ":" + newCollectionId + ":" + offset);
        this.newCollectionId = newCollectionId;
        this.repositoryName = repoName;
        this.collectionMemberIds = collectionMemberIds;
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
    public void work() throws Exception {
        setStatus("Duplicating");
        if (collectionMemberIds != null) {
            CollectionManager collectionManager = Framework.getLocalService(CollectionManager.class);
            setProgress(new Progress(0, collectionMemberIds.size()));
            initSession();
            for (int i = 0; i < collectionMemberIds.size(); i++) {
                log.trace(String.format("Worker %s, populating Collection %s, processing CollectionMember %s", getId(),
                        newCollectionId, collectionMemberIds.get(i)));
                if (collectionMemberIds.get(i) != null) {
                    DocumentModel collectionMember = session.getDocument(new IdRef(
                            collectionMemberIds.get(i)));
                    if (collectionManager.isCollectable(collectionMember)) {

                        // We want to disable the following listener on a
                        // collection member when it is added to a collection
                        collectionMember.putContextData(
                                DublinCoreListener.DISABLE_DUBLINCORE_LISTENER,
                                true);
                        collectionMember.putContextData(
                                NotificationConstants.DISABLE_NOTIFICATION_SERVICE,
                                true);
                        collectionMember.putContextData(
                                NXAuditEventsService.DISABLE_AUDIT_LOGGER, true);
                        collectionMember.putContextData(
                                VersioningService.DISABLE_AUTO_CHECKOUT, true);

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
