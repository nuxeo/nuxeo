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
 *     "Guillaume Renard"
 */

package org.nuxeo.ecm.automation.core.operations.collections;

import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.collections.api.CollectionManager;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;

@Operation(id = MoveCollectionMemberOperation.ID, category = Constants.CAT_DOCUMENT, label = "Reorder members of a collection",
            description = "Move member1 of a collection right after member2 of the same collection. If member2 is not sepcified,"
                    + " the member1 is moved to first position. Returns true if successfully moved.")
public class MoveCollectionMemberOperation {

    public static final String ID = "Document.MoveCollectionMember";

    @Context
    protected CoreSession session;

    @Context
    protected CollectionManager collectionManager;

    @Param(name = "member1", required = true)
    protected DocumentModel member1;

    @Param(name = "member2", required = false)
    protected DocumentModel member2 = null;

    @OperationMethod
    public boolean run(DocumentModel collection) {
        if (collection == null) {
            throw new IllegalArgumentException("Input cannot be null. Pass a collection as input of run method.");
        }
        return collectionManager.moveMembers(session, collection, member1, member2);
    }

}
