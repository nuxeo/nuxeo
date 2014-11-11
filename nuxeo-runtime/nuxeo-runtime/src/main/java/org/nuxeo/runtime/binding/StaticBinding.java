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

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class StaticBinding implements Binding {

    protected Object obj;
    protected final String bindingKey;

    public StaticBinding(String bindingKey) {
        this(bindingKey, null);
    }

    public StaticBinding(String bindingKey, Object obj) {
        this.obj = obj;
        this.bindingKey = bindingKey;
    }

    public Object get() {
        return this.obj;
    }

    public void set(Object obj) {
        this.obj = obj;
    }

    public String getKey() {
        return bindingKey;
    }

}
