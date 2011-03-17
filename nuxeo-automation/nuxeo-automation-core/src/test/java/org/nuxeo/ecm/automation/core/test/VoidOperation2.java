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
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@Operation(id = "v2")
public class VoidOperation2 {

    @Param(name = "message")
    protected String message;

    @Context
    OperationContext ctx;

    @Context
    CoreSession session;

    @OperationMethod
    public DocumentModel printInfo1() throws Exception {
        // System.out.println("O1:doc:doc: "+doc.getId()+". Session:
        // "+session+". message: "+message);
        Helper.updateContext(ctx, "V2:void:doc", message,
                ((DocumentModel) ctx.getInput()).getPathAsString());
        return session.getRootDocument();
    }

    @OperationMethod
    public DocumentModel printInfo3(String doc) throws Exception {
        // System.out.println("O1:ref:doc: "+ref+". Session: "+session+".
        // message: "+message);
        Helper.updateContext(ctx, "V2:string:doc", message, doc);
        return session.getRootDocument();
    }

}
