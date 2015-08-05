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

package org.nuxeo.ecm.core.transientstore;

import java.lang.reflect.Constructor;

import org.nuxeo.common.xmap.XMap;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.transientstore.api.TransientStore;

/**
 * {@link XMap} decriptor for representing the Configuration of a {@link TransientStore}
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 * @since 7.2
 */
@XObject("store")
public class TransientStoreConfig {

    @XNode("@name")
    String name;

    String l1name;

    String l2name;

    @XNode("@name")
    void injectName(String value) {
        name = value;
        l1name = name.concat("L1");
        l2name = name.concat("L2");
    }

    public String getName() {
        return name;
    }

    // target size that ideally should never be exceeded
    long targetMaxSize = -1;

    @XNode("targetMaxSizeMB")
    public void setTargetMaxSizeInMB(int size) {
        targetMaxSize = size * 1024 * 1024;
    }

    // size that must never be exceeded
    long absoluteMaxSize = -1;

    @XNode("absoluteMaxSizeMB")
    public void setAbsoluteMaxSizeInMB(int size) {
        absoluteMaxSize = size * 1024 * 1024;
    }

    @XNode("minimalRetention")
    int minimalRetention = 10;

    Constructor<? extends TransientStore> factory;

    Class<? extends TransientStore> implClass = SimpleTransientStore.class;

    @XNode("@class")
    void injectFactory(Class<? extends TransientStore> type) {
        try {
            factory = type.getDeclaredConstructor(TransientStoreConfig.class);
        } catch (ReflectiveOperationException cause) {
            throw new NuxeoException("Cannot setup transient store factory from " + type, cause);
        }
    }

    protected TransientStore store;

    void init() {
        try {
            store = factory.newInstance(this);
            store.init();
        } catch (ReflectiveOperationException | RuntimeException cause) {
            throw new NuxeoException("Cannot init transient store " + name, cause);
        }
    }

    void shutdown() {
        try {
            store.shutdown();
        } catch (RuntimeException cause) {
            throw new NuxeoException("Cannot shutdown transient store " + name, cause);
        } finally {
            store = null;
        }
    }

}
