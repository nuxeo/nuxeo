/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     matic
 */
package org.nuxeo.common.jndi;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

public class InitContextAccessor extends InitialContext {

    protected InitContextAccessor() throws NamingException {
        super(false); // lazy mode is breaking jboss
    }

    public boolean isAvailable() {
        try {
            return getDefaultInitCtx() != null;
        } catch (NamingException e) {
            return false;
        }
    }

    protected Context _getWritableInitCtx() {
        
        return _getInitCtx();
    }

    protected Context _getInitCtx() {
        try {
            return new InitContextAccessor().getDefaultInitCtx();
        } catch (NamingException e) {
            return null;
        }
    }

    public static final String ENV_CTX_NAME = "java:comp/env";

    public static Context getInitCtx() {
        try {
            return new InitContextAccessor()._getInitCtx();
        } catch (NamingException e) {
            return null;
        }
    }
    
    public static boolean isWritable(Context ctx) {
        try {
            ctx.bind("IsWritable", "is-writable");
            ctx.unbind("IsWritable");
        } catch (NamingException e) {
            return false;
        }
        return true;
    }
    
}