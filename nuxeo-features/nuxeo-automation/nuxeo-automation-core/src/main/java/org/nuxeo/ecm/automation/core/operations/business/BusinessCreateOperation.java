/*
 * Copyright (c) 2006-2013 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Vladimir Pasquier <vpasquier@nuxeo.com>
 *     Stéphane Lacoin <slacoin@nuxeo.com>
 */
package org.nuxeo.ecm.automation.core.operations.business;

import java.util.Map.Entry;

import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.operations.business.adapter.BusinessAdapter;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DataModel;
import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * This operation map pojo client side to document adapter server side and
 * create NX document assuming that pojo and adapter have both properties in
 * common.
 *
 * @since 5.7
 */
@Operation(id = BusinessCreateOperation.ID, category = Constants.CAT_BUSINESS, label = "BusinessCreateOperation", description = "This operation map pojo client side to document adapter server side and create NX document assuming that pojo and adapter have both properties in common.", addToStudio = false)
public class BusinessCreateOperation {

    public static final String ID = "Business.BusinessCreateOperation";

    @Context
    protected CoreSession session;

    @Param(name = "parentPath", required = true)
    protected String parentPath;

    @Param(name = "name", required = true)
    protected String name;

    @OperationMethod
    public BusinessAdapter run(BusinessAdapter input) throws ClientException,
            DocumentException {
        DocumentModel document = input.getDocument();
        DocumentModel createDocumentModel = session.createDocumentModel(
                parentPath, name, input.getType());

        for (Entry<String, DataModel> entry : createDocumentModel.getDataModels().entrySet()) {
            DataModel dataModel = document.getDataModel(entry.getKey());
            entry.getValue().setMap(dataModel.getMap());
        }

        document = session.createDocument(document);
        return document.getAdapter(input.getClass());
    }

}
