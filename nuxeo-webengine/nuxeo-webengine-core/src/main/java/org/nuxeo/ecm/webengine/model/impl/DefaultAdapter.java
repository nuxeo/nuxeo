/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.webengine.model.impl;

import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.nuxeo.ecm.webengine.model.AdapterResource;
import org.nuxeo.ecm.webengine.model.Resource;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class DefaultAdapter extends AbstractResource<ResourceTypeImpl> implements AdapterResource {

    public DefaultAdapter() {
    }

    @Override
    public boolean isAdapter() {
        return true;
    }

    @Override
    public Resource getTarget() {
        return prev;
    }

    /**
     * TODO: is this wanted? Adapter of adapters?
     */
    @Path(value = "@{segment}")
    public AdapterResource disptachAdapter(@PathParam("segment") String adapterName) {
        return ctx.newAdapter(this, adapterName);
    }

}
