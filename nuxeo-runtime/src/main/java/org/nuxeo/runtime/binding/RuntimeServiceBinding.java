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

import org.nuxeo.runtime.model.ComponentInstance;


/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class RuntimeServiceBinding implements Binding {

    protected final ComponentInstance comp;
    protected final Class<?> serviceClass;
    protected final String bindingKey;

    public RuntimeServiceBinding(String bindingKey, ComponentInstance comp, Class<?> serviceClass) {
        this.comp = comp;
        this.serviceClass = serviceClass;
        this.bindingKey = bindingKey;
    }

    public Object get() {
        return comp.getAdapter(serviceClass);
    }

    public String getKey() {
        return bindingKey;
    }

}
