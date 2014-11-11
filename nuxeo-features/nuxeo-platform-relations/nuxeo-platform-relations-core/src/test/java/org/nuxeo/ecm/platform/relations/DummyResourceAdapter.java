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
 * $Id: DummyResourceAdapter.java 28498 2008-01-05 11:46:25Z sfermigier $
 */

package org.nuxeo.ecm.platform.relations;

import java.io.Serializable;
import java.util.Map;

import org.nuxeo.ecm.platform.relations.api.QNameResource;
import org.nuxeo.ecm.platform.relations.api.Resource;
import org.nuxeo.ecm.platform.relations.api.impl.AbstractResourceAdapter;
import org.nuxeo.ecm.platform.relations.api.impl.QNameResourceImpl;

public class DummyResourceAdapter extends AbstractResourceAdapter {

    @Override
    public Serializable getResourceRepresentation(Resource resource,
            Map<String, Serializable> context) {
        if (resource.isQNameResource()) {
            return new DummyResourceLike(
                    ((QNameResource) resource).getLocalName());
        } else {
            return null;
        }
    }

    @Override
    public Resource getResource(Serializable object,
            Map<String, Serializable> context) {
        DummyResourceLike resourceLike = (DummyResourceLike) object;
        return new QNameResourceImpl(namespace, resourceLike.getId());
    }

    @Override
    public Class<?> getKlass() {
        return DummyResourceLike.class;
    }

}
