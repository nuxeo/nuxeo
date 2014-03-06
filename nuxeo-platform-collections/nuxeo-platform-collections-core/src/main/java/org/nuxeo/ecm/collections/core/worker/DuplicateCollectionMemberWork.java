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

import org.nuxeo.ecm.collections.api.CollectionManager;
import org.nuxeo.ecm.collections.core.adapter.CollectionMember;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.work.AbstractWork;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 5.9.3
 */
public class DuplicateCollectionMemberWork extends AbstractWork {

    public DuplicateCollectionMemberWork(final String repoName,
            final String newCollectionId, final List<String> collectionMemberIds, final int offset) {
        super(repoName + ":" + newCollectionId + ":" + offset);
        this.newCollectionId = newCollectionId;
        setDocuments(repoName, collectionMemberIds);
    }

    public static final String CATEGORY = "duplicateCollectionMember";

    protected static final long serialVersionUID = 4985374651436954280L;

    protected static final String TITLE = "Duplicate CollectionMember Work";

    protected String newCollectionId;

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
        if (docIds != null) {
            CollectionManager collectionManager = Framework.getLocalService(CollectionManager.class);
            setProgress(new Progress(0, docIds.size()));
            initSession();
            for (int i = 0; i < docIds.size(); i++) {
                if (docIds.get(i) != null) {
                    DocumentModel doc = session.getDocument(new IdRef(
                            docIds.get(i)));
                    if (collectionManager.isCollectable(doc)) {
                        CollectionMember collectionMember = doc.getAdapter(CollectionMember.class);
                        collectionMember.addToCollection(newCollectionId);
                        session.saveDocument(doc);
                    }
                }
                setProgress(new Progress(i, docIds.size()));
            }
        }
        setStatus("Done");
    }

}
