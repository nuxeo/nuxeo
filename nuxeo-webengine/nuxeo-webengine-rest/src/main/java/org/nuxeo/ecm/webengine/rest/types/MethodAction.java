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

package org.nuxeo.ecm.webengine.rest.types;

import java.lang.reflect.Method;

import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.actions.ActionDescriptor;
import org.nuxeo.ecm.webengine.rest.adapters.WebObject;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class MethodAction extends Action {

    protected Method method;


    public MethodAction(ActionDescriptor desc, Method method) {
        super (desc);
        this.method = method;
    }

    /**
     * @return the method.
     */
    public Method getMethod() {
        return method;
    }

    public Object invoke(WebObject obj) throws WebException {
        checkPermission(obj);
        try {
            return method.invoke(obj);
        } catch (Exception e) {
            throw WebException.wrap("Failed to execute action: "+desc.getId(), e);
        }
    }

}
