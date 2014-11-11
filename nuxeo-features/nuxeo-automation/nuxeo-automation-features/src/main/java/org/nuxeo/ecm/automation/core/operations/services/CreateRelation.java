/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
@Operation(id = CreateRelation.ID, category = Constants.CAT_SERVICES, label = "Create Relation", description = "Create a relation between 2 documents. The subject of the relation will be the input of the operation and the object of the relation will be retrieved from the context using the 'object' field. The 'predicate' field specifies the relation predicate (When using a known predicate, use the full URL like 'purl.org/dc/terms/IsBasedOn', unknown predicates will be treated as plain strings and be the same on the subject and object). The 'outgoing' flag indicates the direction of the relation - the default is false which means the relation will go from the input object to the object specified as 'object' parameter. Return back the subject document.")
public class CreateRelation {

    public static final String ID = "Relations.CreateRelation";

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
    public DocumentModel run(DocumentModel doc) throws Exception {
        relations.addRelation(session, doc, object, predicate, outgoing);
        return doc;
    }
}
