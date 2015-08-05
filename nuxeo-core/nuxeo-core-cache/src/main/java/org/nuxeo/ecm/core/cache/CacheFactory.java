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

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.nuxeo.common.xmap.Context;
import org.nuxeo.common.xmap.XMap;
import org.nuxeo.ecm.core.api.NuxeoException;

public interface CacheFactory {

    XMap xmap(CacheDescriptor config);

    default CacheDescriptor clone(CacheDescriptor config) {
        return clone(config.context, config);
    }

    default CacheDescriptor clone(Context context, CacheDescriptor config) {
        return (CacheDescriptor) xmap(config).load(context, config.document);
    }

    void merge(CacheDescriptor src, CacheDescriptor dst);

    boolean isInstanceType(Class<? extends Cache> type);

    boolean isConfigType(Class<? extends CacheDescriptor> type);

    default CacheDescriptor createConfig(Context context, String name) {
        CacheDescriptor config = createConfig(name);
        XMap xmap = xmap(config);
        try {
            String xml = xmap.toXML(config);
            return (CacheDescriptor) xmap.load(context, new ByteArrayInputStream(xml.getBytes()));
        } catch (IOException cause) {
            throw new NuxeoException("Cannot refetch config", cause);
        }

    }

    CacheDescriptor createConfig(String name);

    Cache createCache(CacheDescriptor config);

    void destroyCache(Cache cache);


}
