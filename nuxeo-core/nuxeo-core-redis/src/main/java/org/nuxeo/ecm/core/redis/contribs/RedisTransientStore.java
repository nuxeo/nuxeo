/*
 * (C) Copyright 2015 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.core.redis.contribs;

import org.nuxeo.ecm.core.cache.Cache;
import org.nuxeo.ecm.core.transientstore.AbstractTransientStore;
import org.nuxeo.ecm.core.transientstore.api.StorageEntry;
import org.nuxeo.ecm.core.transientstore.api.TransientStore;

/**
 * Redis implementation (i.e. Cluster Aware) implementation of the {@link TransientStore}
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 * @since 7.2
 */

public class RedisTransientStore extends AbstractTransientStore {

    public RedisTransientStore() {
    }

    @Override
    protected void incrementStorageSize(StorageEntry entry) {
        // XXX
    }

    @Override
    protected void decrementStorageSize(StorageEntry entry) {
        // XXX
    }

    @Override
    protected long getStorageSize() {
        return 0;
    }

    @Override
    protected void setStorageSize(long newSize) {
        // XXX
    }

    @Override
    public Class<? extends Cache> getCacheImplClass() {
        // TODO Auto-generated method stub
        // return null;
        throw new UnsupportedOperationException();
    }

}
