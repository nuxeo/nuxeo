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

package org.nuxeo.ecm.core.transientstore.api;

import java.io.IOException;

import org.nuxeo.common.annotation.Experimental;

/**
 * Service to expose access to {@link TransientStore}
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 * @since 7.2
 */
@Experimental(comment="https://jira.nuxeo.com/browse/NXP-16577")
public interface TransientStoreService {

    /**
     * Retrieve a {@link TransientStore} by it's name
     *
     * @param name the name of the target {@link TransientStore}
     *
     * @return the target {@link TransientStore} or null if not found
     */
    TransientStore getStore(String name);

    /**
     * Read configuration for the target named {@link TransientStore}
     *
     * @param name the name of the target {@link TransientStore}
     * @return the {@link TransientStoreConfig} of the target store or null if not found
     *
     * @throws IOException
     */
    TransientStoreConfig getStoreConfig(String name) throws IOException;

    /**
     * Dynamically register a new {@link TransientStore} from it's config
     *
     * @param config the {@link TransientStoreConfig} for the new {@link TransientStore}
     * @return the newly registered {@link TransientStore}
     */
    TransientStore registerStore(TransientStoreConfig config);

    /**
     * Triggers Garbage collecting of all {@link TransientStore}
     *
     */
    void doGC();
}
