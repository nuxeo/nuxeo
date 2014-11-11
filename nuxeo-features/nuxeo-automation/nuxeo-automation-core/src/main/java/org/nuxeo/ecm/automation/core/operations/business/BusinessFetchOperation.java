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

import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.operations.business.adapter.BusinessAdapter;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;

/**
 * This operation map pojo client side to document adapter server side and
 * fetch the related NX document.
 *
 * @since 5.7
 */
@Operation(id = BusinessFetchOperation.ID, category = Constants.CAT_BUSINESS, label = "BusinessFetchOperation", description = "This operation map pojo client side to document adapter server side and fetch the related NX document.", addToStudio = false)
public class BusinessFetchOperation {

    public static final String ID = "Business.BusinessFetchOperation";

    @Context
    protected CoreSession session;

    @OperationMethod
    public BusinessAdapter run(BusinessAdapter input) throws ClientException,
            ClassNotFoundException {
        DocumentModel document = session.getDocument(new IdRef(input.getId()));
        return document.getAdapter(input.getClass());
    }
}
