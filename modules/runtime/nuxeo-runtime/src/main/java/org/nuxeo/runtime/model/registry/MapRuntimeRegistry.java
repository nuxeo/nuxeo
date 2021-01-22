/*
 * (C) Copyright 2021 Nuxeo (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.runtime.model.registry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.common.xmap.registry.MapRegistry;
import org.nuxeo.runtime.RuntimeMessage.Level;
import org.nuxeo.runtime.RuntimeMessage.Source;

/**
 * {@link MapRegistry} with additional runtime-related features.
 *
 * @since 11.5
 */
public class MapRuntimeRegistry extends MapRegistry implements RuntimeRegistryLogger {

    private static final Logger log = LogManager.getLogger(MapRuntimeRegistry.class);

    @Override
    protected void logError(String message, Throwable t, String extensionId, String id) {
        log.error(message, t);
        addRuntimeMessage(Level.ERROR, message, Source.EXTENSION, extensionId);
    }

    @Override
    protected void logWarn(String message, String extensionId, String id) {
        log.warn(message);
        addRuntimeMessage(Level.WARNING, message, Source.EXTENSION, extensionId);
    }

}
