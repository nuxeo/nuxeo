/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo
 */

package org.nuxeo.ecm.automation.core.operations.users;

import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.operations.services.directory.AbstractDirectoryOperation;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.usermanager.UserManager;

/**
 * Fetch a user from {@link UserManager} and return it as a
 * {@link DocumentModel}. Using the DocumentModel rather that a POJO allow to
 * also fetch custom properties.
 * 
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 * @since 5.7
 */
@Operation(id = GetNuxeoPrincipal.ID, category = Constants.CAT_USERS_GROUPS, label = "Get Nuxeo Principal", description = "Retrieve Nuxeo principal and export it as a DocumentModel, if login parameter is not set the Operation will return informations about the current user, otherwise Directory Administration rights are required.")
public class GetNuxeoPrincipal extends AbstractDirectoryOperation {

    public static final String ID = "NuxeoPrincipal.Get";

    @Context
    protected UserManager umgr;

    @Param(name = "login", required = false)
    protected String login;

    @Context
    protected OperationContext ctx;

    @OperationMethod
    public DocumentModel run() throws Exception {

        if (login == null || login.isEmpty()) {
            return umgr.getUserModel(ctx.getPrincipal().getName());
        } else {
            validateCanManageDirectories(ctx);
            return umgr.getUserModel(login);
        }
    }

}
