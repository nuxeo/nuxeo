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
package org.nuxeo.ecm.automation.core.test;

import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.core.api.CoreSession;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@Operation(id = "runOnListItem")
public class RunOnListItem {

    @Context
    OperationContext ctx;

    @Context
    CoreSession session;

    @OperationMethod
    public void printInfo() throws Exception {
        String user = (String) ctx.get("item");
        String result = (String) ctx.get("result");
        if (result == null) {
            result = user;
        } else {
            result += ", " + user;
        }
        ctx.put("result", result);
    }

}
