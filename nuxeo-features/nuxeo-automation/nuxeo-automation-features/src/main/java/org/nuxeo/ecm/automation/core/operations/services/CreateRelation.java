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

import java.util.ArrayList;

import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.collectors.DocumentModelCollector;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.relations.api.QNameResource;
import org.nuxeo.ecm.platform.relations.api.RelationManager;
import org.nuxeo.ecm.platform.relations.api.Resource;
import org.nuxeo.ecm.platform.relations.api.Statement;
import org.nuxeo.ecm.platform.relations.api.impl.ResourceImpl;
import org.nuxeo.ecm.platform.relations.api.impl.StatementImpl;
import org.nuxeo.ecm.platform.relations.api.util.RelationConstants;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@Operation(id = CreateRelation.ID, category = Constants.CAT_SERVICES, label = "Create Relation", description = "Create a relation between 2 documents. The subject of the relation will be the input of the operation and the object of the relation will be retrieved from the context using the 'object' field. The 'predicate' field specify the relation predicate. Return back the subject document.")
public class CreateRelation {

    public static final String ID = "Relations.CreateRelation";

    @Context
    protected CoreSession session;

    @Context
    protected RelationManager relations;

    @Param(name = "object")
    protected DocumentModel object;

    @Param(name = "predicate")
    // TODO use a combo box?
    protected String predicate;

    @OperationMethod(collector = DocumentModelCollector.class)
    public DocumentModel run(DocumentModel doc) throws Exception {
        QNameResource subject = getDocumentResource(doc);
        QNameResource obj = getDocumentResource(object);
        Resource predicate = getPredicate();
        ArrayList<Statement> stmts = new ArrayList<Statement>();
        stmts.add(new StatementImpl(subject, predicate, obj));
        relations.add(RelationConstants.GRAPH_NAME, stmts);
        return doc;
    }

    protected QNameResource getDocumentResource(DocumentModel document)
            throws ClientException {
        return (QNameResource) relations.getResource(
                RelationConstants.DOCUMENT_NAMESPACE, document, null);
    }

    protected Resource getPredicate() {
        return predicate != null && predicate.length() > 0 ? new ResourceImpl(
                predicate) : null;
    }

}
