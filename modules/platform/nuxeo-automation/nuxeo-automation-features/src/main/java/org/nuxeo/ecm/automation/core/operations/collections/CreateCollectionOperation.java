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
 *     <a href="mailto:glefevre@nuxeo.com">Gildas</a>
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

/**
 * Class for the operation to create a Collection.
 *
 * @since 5.9.4
 */
@Operation(id = CreateCollectionOperation.ID, category = Constants.CAT_DOCUMENT, label = "Create a collection", description = "Create a new collection. "
        + "This is returning the document serialization of the created collection.", aliases = { "Collection.CreateCollection" })
public class CreateCollectionOperation {

    public static final String ID = "Collection.Create";

    @Context
    protected CoreSession session;

    @Context
    protected CollectionManager collectionManager;

    @Param(name = "name")
    protected String name;

    @Param(name = "description", required = false)
    protected String description;

    @OperationMethod
    public DocumentModel run(DocumentModel doc) {
        return collectionManager.createCollection(session, name, description, doc.getPathAsString());
    }

    @OperationMethod
    public DocumentModel run() {
        return collectionManager.createCollection(session, name, description, null);
    }
}
