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

package org.nuxeo.ecm.webengine.rest.domains;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.webengine.RootDescriptor;
import org.nuxeo.ecm.webengine.exceptions.WebSecurityException;
import org.nuxeo.ecm.webengine.security.Guard;
import org.nuxeo.ecm.webengine.security.GuardDescriptor;
import org.nuxeo.runtime.model.Adaptable;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
@XObject("domain")
public class DomainDescriptor {

    @XNode("@id")
    public String id;

    /** the name of the type or he class of the resource to serve */
    @XNode("@type")
    public String type;

    @XNode("@path")
    public String path;

    @XNode("@extends")
    public String base;

    @XNode("root")
    public String root;

    @XNode("errorPage")
    public String errorPage = "error.ftl";

    @XNode("indexPage")
    public String indexPage = "index.ftl";

    @XNode("defaultPage")
    public String defaultPage = "default.ftl";

    @XNode("script-extension")
    public String scriptExtension = "groovy";

    @XNode("template-extension")
    public String templateExtension = "ftl";

    @XNodeList(value="roots/root", type=ArrayList.class, componentType=RootDescriptor.class, nullByDefault=true)
    public List<RootDescriptor> roots;

    @XNode("locator")
    public String locator;

    @XNode("default")
    public boolean isDefault;

    @XNode("permission")
    public  GuardDescriptor guardDescriptor;

    private Guard guard;

    public void checkPermission(Adaptable adaptable) throws WebSecurityException {
        if (!getGuard().check(adaptable)) {
            throw new WebSecurityException("Access Restricted");
        }
    }

    public Guard getGuard() {
        if (guard == null) {
            try {
                guard = guardDescriptor != null? guardDescriptor.getGuard() : Guard.DEFAULT;
            } catch (ParseException e) {
                e.printStackTrace();
                return null;
            }
        }
        return guard;
    }

}
