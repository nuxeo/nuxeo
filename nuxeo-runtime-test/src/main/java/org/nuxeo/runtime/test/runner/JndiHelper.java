/*
 * JBoss, Home of Professional Open Source
 * Copyright 2005, JBoss Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.nuxeo.runtime.test.runner;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;

/**
 * helper for common jndi operations copied from jboss Util class
 */
public class JndiHelper {

    /**
     * Create a subcontext including any intermediate contexts.
     *
     * @param ctx the parent JNDI Context under which value will be bound
     * @param name the name relative to ctx of the subcontext.
     * @return The new or existing JNDI subcontext
     * @throws javax.naming.NamingException on any JNDI failure
     */
    public static Context createSubcontext(Context ctx, String name)
            throws NamingException {
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
    public static Context createSubcontext(Context ctx, Name name)
            throws NamingException {
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
     * Bind val to name in ctx, and make sure that all intermediate contexts
     * exist
     *
     * @param ctx the parent JNDI Context under which value will be bound
     * @param name the name relative to ctx where value will be bound
     * @param value the value to bind.
     * @throws NamingException for any error
     */
    public static void bind(Context ctx, String name, Object value)
            throws NamingException {
        Name n = ctx.getNameParser("").parse(name);
        bind(ctx, n, value);
    }

    /**
     * Bind val to name in ctx, and make sure that all intermediate contexts
     * exist
     *
     * @param ctx the parent JNDI Context under which value will be bound
     * @param name the name relative to ctx where value will be bound
     * @param value the value to bind.
     * @throws NamingException for any error
     */
    public static void bind(Context ctx, Name name, Object value)
            throws NamingException {
        int size = name.size();
        String atom = name.get(size - 1);
        Context parentCtx = createSubcontext(ctx, name.getPrefix(size - 1));
        parentCtx.bind(atom, value);
    }

    /**
     * Rebind val to name in ctx, and make sure that all intermediate contexts
     * exist
     *
     * @param ctx the parent JNDI Context under which value will be bound
     * @param name the name relative to ctx where value will be bound
     * @param value the value to bind.
     * @throws NamingException for any error
     */
    public static void rebind(Context ctx, String name, Object value)
            throws NamingException {
        Name n = ctx.getNameParser("").parse(name);
        rebind(ctx, n, value);
    }

    /**
     * Rebind val to name in ctx, and make sure that all intermediate contexts
     * exist
     *
     * @param ctx the parent JNDI Context under which value will be bound
     * @param name the name relative to ctx where value will be bound
     * @param value the value to bind.
     * @throws NamingException for any error
     */
    public static void rebind(Context ctx, Name name, Object value)
            throws NamingException {
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
                e.printStackTrace();
                break;
            }
        }
    }

}
