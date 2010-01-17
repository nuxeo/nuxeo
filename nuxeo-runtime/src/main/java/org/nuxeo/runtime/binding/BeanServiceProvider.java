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
 *
 * $Id$
 */

package org.nuxeo.runtime.binding;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class BeanServiceProvider extends AbstractServiceProvider {

    public static final String PREFIX = "nxservice";
    public static final String REMOTE_SUFFIX = "remote";
    public static final String LOCAL_SUFFIX = "local";

    protected Context ctx;
    protected boolean tryLocalFirst = true;

    /**
     * This constructor should be used on server nodes.
     * It will initialize the provider by using the default initial context.
     * (as configured by the server application)
     */
    public BeanServiceProvider() throws NamingException {
        this(new InitialContext(), false);
    }

    /**
     * This constructor should be used on clients.
     *
     * @param ctx the initial naming context
     */
    public BeanServiceProvider(InitialContext ctx) throws NamingException {
        this(ctx, true);
    }

    /**
     * This constructor should e used on clients that are using auto configuration.
     * The JNDI environment should be passed by the client.
     *
     * @param ctx the initial naming context
     */
    public BeanServiceProvider(InitialContext ctx, boolean tryLocalFirst) throws NamingException {
        this.tryLocalFirst = tryLocalFirst;
        JndiName comp = new JndiName(PREFIX);
        try {
            this.ctx = (Context) ctx.lookup(comp);
         } catch(NameNotFoundException e) {
            this.ctx = ctx.createSubcontext(comp);
         }
    }

    public void destroy() {
        ctx = null;
    }

    public Object getService(Class<?> serviceClass, String bindingKey) {
        try {
            Object obj;
            if (tryLocalFirst) {
                JndiName name = new JndiName(bindingKey, LOCAL_SUFFIX);
                obj = ctx.lookup(name); // throws an exception if not found
                if (manager != null) {
                    JndiBinding binding = new JndiBinding(bindingKey, ctx, name);
                    manager.registerBinding(bindingKey, binding);
                    return obj;
                }
            }
            JndiName name = new JndiName(bindingKey, REMOTE_SUFFIX);
            obj = ctx.lookup(name); // throws an exception if not found
            if (manager != null) {
                JndiBinding binding = new JndiBinding(bindingKey, ctx, name);
                manager.registerBinding(bindingKey, binding);
            }
            return obj;
        } catch (NamingException e) {
            // do nothing
        }
        return null;
    }

}
