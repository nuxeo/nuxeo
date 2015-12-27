/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
     * Try writing in JNDI space
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
