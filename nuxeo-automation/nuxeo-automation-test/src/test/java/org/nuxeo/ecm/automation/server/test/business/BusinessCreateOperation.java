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

/**
 * This operation map pojo client side to document adapter server side and
 * create NX document assuming that pojo and adapter have both properties in
 * common. Parameter 'adapter' is the canonical name adapter class to map server
 * side.
 *
 * @since 5.7
 */
@Operation(id = BusinessCreateOperation.ID, category = Constants.CAT_EXECUTION, label = "BusinessCreateOperation", description = "This operation map pojo client side to document adapter server side and create NX document assuming that pojo and adapter have both properties in common. Parameter 'adapter' is the canonical name adapter class to map server side.")
public class BusinessCreateOperation {

    public static final String ID = "Operation.BusinessCreateOperation";

    @Context
    protected CoreSession session;

    @Param(name = "parentPath", required = true)
    protected String parentPath;

    @Param(name = "name", required = true)
    protected String name;

    @Param(name = "type", required = true)
    protected String type;

    @Param(name = "adapter", required = true)
    protected String adapter;

    @OperationMethod
    public Object run(Object input) throws ClientException,
            ClassNotFoundException {
        DocumentModel document = session.createDocumentModel(parentPath, name,
                type);
        Object adapter = document.getAdapter(Class.forName(this.adapter));
        mapObject(input, adapter);
        document = session.createDocument(document);
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
