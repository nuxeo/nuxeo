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

package org.nuxeo.ecm.webengine.model.impl;

import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.model.AdapterResource;
import org.nuxeo.ecm.webengine.model.Resource;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class DefaultAdapter extends AbstractResource<ResourceTypeImpl> implements AdapterResource {

    public DefaultAdapter() {
    }

    @Override
    public boolean isAdapter() {
        return true;
    }

    public Resource getTarget() {
        return prev;
    }

    /**
     * TODO: is this wanted? Adapter of adapters?
     * @param adapterName
     * @return
     * @throws WebException
     */
    @Path(value="@{segment}")
    public AdapterResource disptachAdapter(@PathParam("segment") String adapterName) {
        return ctx.newAdapter(this, adapterName);
    }

}
