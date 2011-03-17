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

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class Helper {

    private Helper() {
    }

    public static void updateContext(OperationContext ctx, String id,
            String message, String title) {
        updateTestParam(ctx, "chain", id);
        updateTestParam(ctx, "message", message);
        updateTestParam(ctx, "title", title);
    }

    protected static void updateTestParam(OperationContext ctx, String param,
            String value) {
        String v = (String) ctx.get(param);
        if (v == null) {
            v = value;
        } else {
            v += "," + value;
        }
        ctx.put(param, v);
    }

}
