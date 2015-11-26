/*
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
 *
 * Contributors:
 */
package org.nuxeo.ecm.platform.rendition.service;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * Descriptor for a stored rendition manager.
 *
 * @since 8.1
 */
@XObject("storedRenditionManager")
public class StoredRenditionManagerDescriptor {

    @XNode("@class")
    protected Class<StoredRenditionManager> clazz;

    protected StoredRenditionManager instance;

    protected synchronized StoredRenditionManager getStoredRenditionManager() {
        if (instance == null) {
            try {
                instance = clazz.newInstance();
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException("Cannot create StoredRenditionManager", e);
            }
        }
        return instance;
    }

}
