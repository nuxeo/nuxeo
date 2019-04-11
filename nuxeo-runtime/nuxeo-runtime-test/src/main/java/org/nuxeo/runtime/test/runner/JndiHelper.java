/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.runtime.test.runner;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * helper for common jndi operations copied from jboss Util class
 */
public class JndiHelper {

    private static final Log log = LogFactory.getLog(JndiHelper.class);

    /**
     * Create a subcontext including any intermediate contexts.
     *
     * @param ctx the parent JNDI Context under which value will be bound
     * @param name the name relative to ctx of the subcontext.
     * @return The new or existing JNDI subcontext
     * @throws javax.naming.NamingException on any JNDI failure
     */
    public static Context createSubcontext(Context ctx, String name) throws NamingException {
        Name n = ctx.getNameParser("").parse(name);
        return createSubcontext(ctx, n);
    }

    /**
     * Create a subcontext including any intermediate contexts.
     *
     * @param ctx the parent JNDI Context under which value will be bound
     * @param name the name relative to ctx of the subcontext.
     * @return The new or existing JNDI subcontext
     * @throws NamingException on any JNDI failure
     */
    public static Context createSubcontext(Context ctx, Name name) throws NamingException {
        Context subctx = ctx;
        for (int pos = 0; pos < name.size(); pos++) {
            String ctxName = name.get(pos);
            try {
                subctx = (Context) ctx.lookup(ctxName);
            } catch (NameNotFoundException e) {
                subctx = ctx.createSubcontext(ctxName);
            }
            // The current subctx will be the ctx for the next name component
            ctx = subctx;
        }
        return subctx;
    }

    /**
     * Bind val to name in ctx, and make sure that all intermediate contexts exist
     *
     * @param ctx the parent JNDI Context under which value will be bound
     * @param name the name relative to ctx where value will be bound
     * @param value the value to bind.
     * @throws NamingException for any error
     */
    public static void bind(Context ctx, String name, Object value) throws NamingException {
        Name n = ctx.getNameParser("").parse(name);
        bind(ctx, n, value);
    }

    /**
     * Bind val to name in ctx, and make sure that all intermediate contexts exist
     *
     * @param ctx the parent JNDI Context under which value will be bound
     * @param name the name relative to ctx where value will be bound
     * @param value the value to bind.
     * @throws NamingException for any error
     */
    public static void bind(Context ctx, Name name, Object value) throws NamingException {
        int size = name.size();
        String atom = name.get(size - 1);
        Context parentCtx = createSubcontext(ctx, name.getPrefix(size - 1));
        parentCtx.bind(atom, value);
    }

    /**
     * Rebind val to name in ctx, and make sure that all intermediate contexts exist
     *
     * @param ctx the parent JNDI Context under which value will be bound
     * @param name the name relative to ctx where value will be bound
     * @param value the value to bind.
     * @throws NamingException for any error
     */
    public static void rebind(Context ctx, String name, Object value) throws NamingException {
        Name n = ctx.getNameParser("").parse(name);
        rebind(ctx, n, value);
    }

    /**
     * Rebind val to name in ctx, and make sure that all intermediate contexts exist
     *
     * @param ctx the parent JNDI Context under which value will be bound
     * @param name the name relative to ctx where value will be bound
     * @param value the value to bind.
     * @throws NamingException for any error
     */
    public static void rebind(Context ctx, Name name, Object value) throws NamingException {
        int size = name.size();
        String atom = name.get(size - 1);
        Context parentCtx = createSubcontext(ctx, name.getPrefix(size - 1));
        parentCtx.rebind(atom, value);
    }

    /**
     * Unbinds a name from ctx, and removes parents if they are empty
     *
     * @param ctx the parent JNDI Context under which the name will be unbound
     * @param name The name to unbind
     * @throws NamingException for any error
     */
    public static void unbind(Context ctx, String name) throws NamingException {
        unbind(ctx, ctx.getNameParser("").parse(name));
    }

    /**
     * Unbinds a name from ctx, and removes parents if they are empty
     *
     * @param ctx the parent JNDI Context under which the name will be unbound
     * @param name The name to unbind
     * @throws NamingException for any error
     */
    public static void unbind(Context ctx, Name name) throws NamingException {
        ctx.unbind(name); // unbind the end node in the name
        int sz = name.size();
        // walk the tree backwards, stopping at the domain
        while (--sz > 0) {
            Name pname = name.getPrefix(sz);
            try {
                ctx.destroySubcontext(pname);
            } catch (NamingException e) {
                log.error(e, e);
                break;
            }
        }
    }

}
