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
package org.nuxeo.runtime.jtajca;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * Utility class used for testing JNDI space provider.
 *
 * @author Stephane Lacoin (aka matic)
 * @since 5.5
 *
 */
public class InitialContextAccessor extends InitialContext {

    public static final String ENV_CTX_NAME = "java:comp/env";

    protected InitialContextAccessor() throws NamingException {
        super(false); // lazy mode is breaking jboss
    }

    protected Context _getInitCtx() {
        try {
            return getURLOrDefaultInitCtx("java:");
        } catch (NamingException e) {
            return null;
        }
    }

    /**
     * Check for JNDI space availability
     *
     * @return true if JNDI space exists
     */
    public static boolean isAvailable() {
        try {
            return new InitialContextAccessor().getDefaultInitCtx() != null;
        } catch (NamingException e) {
            return false;
        }
    }

    /**
     *  Try writing in JNDI space
     *
     * @param ctx
     * @return true if JNDI space is writable
     */
    public static boolean isWritable(Context ctx, String prefix) {
        try {
            final String name = prefix.concat("IsWritable");
            ctx.bind(name, "is-writable");
            ctx.unbind(name);
        } catch (NamingException e) {
            return false;
        }
        return true;
    }

    /**
     * Get access to the default initial context implementation
     *
     * @return the initial context implementation
     */
    public static Context getInitialContext() {
        try {
            return new InitialContextAccessor()._getInitCtx();
        } catch (NamingException e) {
            return null;
        }
    }

}
