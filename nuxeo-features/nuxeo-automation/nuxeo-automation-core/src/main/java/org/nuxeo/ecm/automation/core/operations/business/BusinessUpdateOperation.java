/*
 * Copyright (c) 2006-2013 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Vladimir Pasquier <vpasquier@nuxeo.com>
 */
package org.nuxeo.ecm.automation.core.operations.business;

import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.operations.business.adapter.BusinessAdapter;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * This operation map pojo client side to document adapter server side and
 * update the related NX document.
 *
 * @since 5.7
 */
@Operation(id = BusinessUpdateOperation.ID, category = Constants.CAT_BUSINESS, label = "BusinessUpdateOperation", description = "This operation map pojo client side to document adapter server side and update the related NX document.", addToStudio = false)
public class BusinessUpdateOperation {

    public static final String ID = "Business.BusinessUpdateOperation";

    @Context
    protected CoreSession session;

    @OperationMethod
    public BusinessAdapter run(BusinessAdapter input) throws ClientException,
            ClassNotFoundException, DocumentException {
        DocumentModel document = input.getDocument();
        session.saveDocument(document);
        return input;
    }

}
