/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.core.schema.registries;

import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.runtime.model.ContributionFragmentRegistry;

/**
 * Registry for core types
 *
 * @since 5.6
 */
public class SchemaTypeRegistry extends ContributionFragmentRegistry<Type> {

    protected final Map<String, Type> types = new HashMap<String, Type>();

    @Override
    public String getContributionId(Type contrib) {
        return contrib.getName();
    }

    @Override
    public void contributionUpdated(String id, Type contrib, Type newOrigContrib) {
        types.put(id, contrib);
    }

    @Override
    public void contributionRemoved(String id, Type origContrib) {
        types.remove(id);
    }

    @Override
    public boolean isSupportingMerge() {
        return false;
    }

    @Override
    public Type clone(Type orig) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void merge(Type src, Type dst) {
        throw new UnsupportedOperationException();
    }

    // custom API

    public Type getType(String name) {
        return types.get(name);
    }

    public Type[] getTypes() {
        return types.values().toArray(new Type[types.size()]);
    }

    public int size() {
        return types.size();
    }

    public void clear() {
        types.clear();
        contribs.clear();
    }

}
