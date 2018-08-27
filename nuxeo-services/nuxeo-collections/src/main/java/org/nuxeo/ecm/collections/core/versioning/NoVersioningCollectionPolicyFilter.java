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
 *     Kevin Leturc <kleturc@nuxeo.com>
 */
package org.nuxeo.ecm.collections.core.versioning;

import java.util.List;

import org.nuxeo.ecm.collections.api.CollectionConstants;
import org.nuxeo.ecm.collections.core.adapter.CollectionMember;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.versioning.VersioningPolicyFilter;

/**
 * Policy filter which disables automatic versioning for collection actions.
 *
 * @since 9.2
 */
public class NoVersioningCollectionPolicyFilter implements VersioningPolicyFilter {

    @Override
    public boolean test(DocumentModel previousDocument, DocumentModel currentDocument) {
        // we don't want to trigger automatic versioning system for documents with 'Collection' facet
        if (currentDocument.hasFacet(CollectionConstants.COLLECTION_FACET)) {
            return true;
        }
        // next tests suppose it's an update, don't apply policies referencing this filter for the creation step
        if (previousDocument == null) {
            return false;
        }
        boolean previousHasMember = previousDocument.hasSchema(CollectionConstants.COLLECTION_MEMBER_SCHEMA_NAME);
        boolean currentHashMember = currentDocument.hasSchema(CollectionConstants.COLLECTION_MEMBER_SCHEMA_NAME);
        if (!previousHasMember && currentHashMember) {
            // case when we add document to a collection
            // here we suppose that add/remove is the only update of document (default behavior of collectionManager)
            return true;
        } else if (previousHasMember && !currentHashMember) {
            // case when we copy a document and re-init all values from collection members
            // here we suppose that add/remove is the only update of document (default behavior of collectionManager)
            return true;
        } else if (previousHasMember && currentHashMember) {
            // we need to check if there was changes in members
            List<String> previousMembers = previousDocument.getAdapter(CollectionMember.class).getCollectionIds();
            List<String> currentMembers = currentDocument.getAdapter(CollectionMember.class).getCollectionIds();
            // here we suppose that add/remove is the only update of document (default behavior of collectionManager)
            return previousMembers.size() != currentMembers.size()
                    // check if ids are the same - in case we edit the ids instead of removing then adding
                    || !currentMembers.containsAll(previousMembers);
        }
        // last case is !previousHasMember && !currentHashMember - nothing related to collections skip
        return false;
    }

}
