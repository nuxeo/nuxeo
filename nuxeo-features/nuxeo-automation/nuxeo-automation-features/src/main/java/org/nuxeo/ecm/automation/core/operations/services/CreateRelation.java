/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.core.operations.services;

import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.collectors.DocumentModelCollector;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.relations.api.DocumentRelationManager;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@Operation(id = CreateRelation.ID, category = Constants.CAT_SERVICES, label = "Create Relation", description = "Create a relation between 2 documents. The subject of the relation will be the input of the operation and the object of the relation will be retrieved from the context using the 'object' field. The 'predicate' field specifies the relation predicate (When using a known predicate, use the full URL like 'http://purl.org/dc/terms/IsBasedOn', unknown predicates will be treated as plain strings and be the same on the subject and object). The 'outgoing' flag indicates the direction of the relation - the default is false which means the relation will go from the input object to the object specified as 'object' parameter. Return back the subject document.", aliases = { "Relations.CreateRelation" })
public class CreateRelation {

    public static final String ID = "Document.AddRelation";

    @Context
    protected CoreSession session;

    @Context
    protected DocumentRelationManager relations;

    @Param(name = "object")
    protected DocumentModel object;

    @Param(name = "predicate")
    // TODO use a combo box?
    protected String predicate;

    @Param(name = "outgoing", required = false, values = "false")
    protected boolean outgoing = false;

    @OperationMethod(collector = DocumentModelCollector.class)
    public DocumentModel run(DocumentModel doc) {
        // `outgoing` parameter indicates inverse relationship, which is
        // the opposite of the outgoing value.
        relations.addRelation(session, doc, object, predicate, !outgoing);
        return doc;
    }
}
