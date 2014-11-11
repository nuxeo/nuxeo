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
import javax.naming.Name;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class JndiBinding implements Binding {

    protected final Context ctx;
    protected final Name name;
    protected final String bindingKey;

    public JndiBinding(String bindingKey, Context ctx, Name name) {
        this.ctx = ctx;
        this.name = name;
        this.bindingKey = bindingKey;
    }

    public Object get() {
        try {
            return ctx.lookup(name);
        } catch (Exception e) {
            return null;
        }
    }

    public String getKey() {
        return bindingKey;
    }

}
