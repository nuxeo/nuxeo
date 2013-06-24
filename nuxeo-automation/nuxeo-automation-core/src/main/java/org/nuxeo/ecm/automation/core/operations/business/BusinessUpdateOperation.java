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

import java.lang.reflect.InvocationTargetException;

import org.apache.commons.beanutils.BeanUtils;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.operations.business.adapter.BusinessAdapter;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;

/**
 * This operation map pojo client side to document adapter server side and
 * update the related NX document.
 *
 * @since 5.7
 */
@Operation(id = BusinessUpdateOperation.ID, category = Constants.CAT_BUSINESS, label = "BusinessUpdateOperation", description = "This operation map pojo client side to document adapter server side and update the related NX document.")
public class BusinessUpdateOperation {

    public static final String ID = "Business.BusinessUpdateOperation";

    @Context
    protected CoreSession session;

    @Param(name = "id", required = true)
    protected String id;

    @OperationMethod
    public BusinessAdapter run(BusinessAdapter input) throws ClientException,
            ClassNotFoundException {
        // TODO: would be nice to get the document to reattach the doc to the
        // session (but cannot access to it from the adapter input)
        DocumentModel document = session.getDocument(new IdRef(id));
        BusinessAdapter adapter = document.getAdapter(input.getClass());
        mapObject(input, adapter);
        document = session.saveDocument(adapter.getDocument());
        return document.getAdapter(input.getClass());
    }

    private void mapObject(Object input, Object adapter) {
        try {
            BeanUtils.copyProperties(adapter, input);
        } catch (InvocationTargetException e) {
            throw new ClientRuntimeException("cannot copy properties", e);
        } catch (IllegalAccessException e) {
            throw new ClientRuntimeException("cannot copy properties", e);
        }
    }
}
