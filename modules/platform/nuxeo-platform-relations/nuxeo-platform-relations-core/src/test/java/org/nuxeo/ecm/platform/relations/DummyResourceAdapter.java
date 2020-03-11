/*
 * (C) Copyright 2007 Nuxeo SA (http://nuxeo.com/) and others.
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
    public Serializable getResourceRepresentation(Resource resource, Map<String, Object> context) {
        if (resource.isQNameResource()) {
            return new DummyResourceLike(((QNameResource) resource).getLocalName());
        } else {
            return null;
        }
    }

    @Override
    public Resource getResource(Serializable object, Map<String, Object> context) {
        DummyResourceLike resourceLike = (DummyResourceLike) object;
        return new QNameResourceImpl(namespace, resourceLike.getId());
    }

    @Override
    public Class<?> getKlass() {
        return DummyResourceLike.class;
    }

}
