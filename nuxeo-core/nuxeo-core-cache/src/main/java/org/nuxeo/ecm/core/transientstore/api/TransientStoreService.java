/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 * Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.core.transientstore.api;

import java.util.Set;

/**
 * Service to expose access to {@link TransientStore}
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 * @since 7.2
 */
public interface TransientStoreService {

    /**
     * Retrieves a {@link TransientStore} by it's name.
     * <p>
     * If the {@link TransientStore} is not found, returns the default one.
     *
     * @param name the name of the target {@link TransientStore}
     * @return the target {@link TransientStore} or the default one if not found
     */
    TransientStore getStore(String name);

    /**
     * Triggers Garbage collecting of all {@link TransientStore}
     */
    void doGC();

    /**
     * Triggers Garbage collecting for a {@link TransientStore}.
     *
     * @param name the name of the target {@link TransientStore}
     * @since 2021.17
     */
    void doGC(String name);

    /**
     * List contributed transient storage names
     *
     * @since 2021.17
     */
    Set<String> listStores();

}
