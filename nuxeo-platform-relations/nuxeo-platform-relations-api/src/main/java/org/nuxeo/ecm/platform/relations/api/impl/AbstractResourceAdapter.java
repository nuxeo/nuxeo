/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: AbstractResourceAdapter.java 22121 2007-07-06 16:33:15Z gracinet $
 */

package org.nuxeo.ecm.platform.relations.api.impl;

import java.io.Serializable;
import java.util.Map;

import org.nuxeo.ecm.platform.relations.api.Resource;
import org.nuxeo.ecm.platform.relations.api.ResourceAdapter;

/**
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 */
public abstract class AbstractResourceAdapter implements ResourceAdapter {

    protected String namespace;

    @Override
    public String getNamespace() {
        return namespace;
    }

    @Override
    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    @Override
    public Class<?> getKlass() {
        return null;
    }

    @Override
    public Resource getResource(Serializable object,
            Map<String, Object> context) {
        return null;
    }

    @Override
    public Serializable getResourceRepresentation(Resource resource,
            Map<String, Object> context) {
        return null;
    }

}
