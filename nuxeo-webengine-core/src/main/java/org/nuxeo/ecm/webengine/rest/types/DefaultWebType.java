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

import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.rest.adapters.WebObject;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class DefaultWebType extends AbstractWebType {

    // fields that need to be resolved
    protected Class<? extends WebObject> klass;
    protected WebType superType;
    protected WebTypeDescriptor desc;
    protected WebTypeManager mgr;

    public DefaultWebType(WebTypeManager mgr, WebTypeDescriptor desc) throws WebException {
        this.desc = desc;
        this.mgr = mgr;
        try {
            superType = mgr.getType(desc.superTypeName);
        } catch (Exception e) {
            WebException.wrap("Failed to resolve type: "+desc.name, e);
        }
    }

    public boolean isDynamic() {
        return false;
    }

    public String getName() {
        return desc.name;
    }

    public Class<? extends WebObject> getObjectClass() throws WebException {
        if (klass == null) {
            klass = resolveObjectClass(mgr, desc.className);
        }
        return klass;
    }

    public WebType getSuperType() {
        return superType;
    }

}
