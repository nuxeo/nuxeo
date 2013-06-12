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
package org.nuxeo.ecm.automation.server.test.business;

import java.lang.reflect.InvocationTargetException;

import org.apache.commons.beanutils.BeanUtils;
import org.nuxeo.ecm.automation.client.AutomationException;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;

/**
 * This operation map pojo client side to document adapter server side and
 * update the related NX document. Parameter 'adapter' is the canonical name
 * adapter class to map server side.
 *
 * @since 5.7
 */
@Operation(id = BusinessUpdateOperation.ID, category = Constants.CAT_EXECUTION, label = "BusinessUpdateOperation", description = "This operation map pojo client side to document adapter server side and update the related NX document. Parameter 'adapter' is the canonical name adapter class to map server side.")
public class BusinessUpdateOperation {

    public static final String ID = "Operation.BusinessUpdateOperation";

    @Context
    protected CoreSession session;

    @Param(name = "id", required = true)
    protected String id;

    @Param(name = "adapter", required = true)
    protected String adapter;

    @OperationMethod
    public Object run(Object input) throws ClientException,
            ClassNotFoundException {
        DocumentModel document = session.getDocument(new IdRef(id));
        Object adapter = document.getAdapter(Class.forName(this.adapter));
        mapObject(input, adapter);
        document = session.saveDocument(document);
        adapter = document.getAdapter(Class.forName(this.adapter));
        mapObject(adapter, input);
        return input;
    }

    private void mapObject(Object input, Object adapter) {
        try {
            BeanUtils.copyProperties(adapter, input);
        } catch (InvocationTargetException e) {
            throw new AutomationException("cannot copy properties", e);
        } catch (IllegalAccessException e) {
            throw new AutomationException("cannot copy properties", e);
        }
    }
}
