/*******************************************************************************
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 ******************************************************************************/
package org.nuxeo.ecm.core.cache;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.runtime.model.ContributionFragmentRegistry;

public class CacheFactoryRegistry extends ContributionFragmentRegistry<CacheFactoryDescriptor> {
     protected final Map<String, CacheFactoryDescriptor> configs = new HashMap<>();

    protected final CacheFactory initFactory = new NoCacheFactory();

    protected CacheFactoryDescriptor initConfig;

    protected CacheFactoryDescriptor defaultConfig;

    @Override
    public String getContributionId(CacheFactoryDescriptor contrib) {
        return contrib.name;
    }

    @Override
    public void contributionUpdated(String id, CacheFactoryDescriptor contrib, CacheFactoryDescriptor newOrigContrib) {
        configs.put(contrib.name, contrib);
        if (initConfig == null) {
            defaultConfig = initConfig = contrib;
        } else if (contrib.isDefault) {
            defaultConfig = contrib;
        }
    }


    @Override
    public void contributionRemoved(String id, CacheFactoryDescriptor origContrib) {
        CacheFactoryDescriptor config = configs.remove(id);
        if (defaultConfig == config) {
            Iterator<CacheFactoryDescriptor> it = configs.values().iterator();
            defaultConfig = it.hasNext() ? it.next() : initConfig;
        }
    }

    @Override
    public CacheFactoryDescriptor clone(CacheFactoryDescriptor orig) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isSupportingMerge() {
        return false;
    }

    @Override
    public void merge(CacheFactoryDescriptor src, CacheFactoryDescriptor dst) {
        throw new UnsupportedOperationException();
    }

    CacheFactoryDescriptor select(String type) {
        CacheFactoryDescriptor config = defaultConfig;
        if (type != null) {
            config = configs.get(type);
        }
        return config;
    }

    public String getType(Class<? extends Cache> clazz) {

        for (CacheFactoryDescriptor each : configs.values()) {
            if (each.factory.isInstanceType(clazz)) {
                return each.name;
            }
        }

        throw new NuxeoException("No cache factories registered with type " + clazz.getName());
    }



}
