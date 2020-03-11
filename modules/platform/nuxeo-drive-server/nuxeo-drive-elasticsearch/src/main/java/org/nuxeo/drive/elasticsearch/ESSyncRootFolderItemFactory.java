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
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.drive.elasticsearch;

import org.nuxeo.drive.adapter.FileSystemItem;
import org.nuxeo.drive.adapter.FolderItem;
import org.nuxeo.drive.service.impl.DefaultSyncRootFolderItemFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.elasticsearch.ElasticSearchConstants;
import org.nuxeo.runtime.api.Framework;

/**
 * Elasticsearch implementation of the {@link DefaultSyncRootFolderItemFactory}.
 *
 * @since 8.3
 */
public class ESSyncRootFolderItemFactory extends DefaultSyncRootFolderItemFactory {

    @Override
    protected FileSystemItem adaptDocument(DocumentModel doc, boolean forceParentItem, FolderItem parentItem,
            boolean relaxSyncRootConstraint, boolean getLockInfo) {
        if (Framework.isBooleanPropertyFalse(ElasticSearchConstants.ES_ENABLED_PROPERTY)) {
            return super.adaptDocument(doc, forceParentItem, parentItem, relaxSyncRootConstraint, getLockInfo);
        } else {
            return new ESSyncRootFolderItem(name, parentItem, doc, relaxSyncRootConstraint, getLockInfo);
        }
    }

}
