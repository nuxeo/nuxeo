/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.core.test;

import org.nuxeo.ecm.automation.OperationContext;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class Helper {

    public static void updateContext(OperationContext ctx, String id, String message, String title) {
        updateTestParam(ctx, "chain", id);
        updateTestParam(ctx, "message", message);
        updateTestParam(ctx, "title", title);
    }

    protected static void updateTestParam(OperationContext ctx, String param, String value) {
        String v = (String)ctx.get(param);
        if (v == null) {
            v = value;
        } else {
            v+=","+value;
        }
        ctx.put(param, v);        
    }
    
}
