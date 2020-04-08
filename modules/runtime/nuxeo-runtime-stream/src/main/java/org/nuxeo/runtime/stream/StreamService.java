/*
 * (C) Copyright 2017 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bdelbosc
 */
package org.nuxeo.runtime.stream;

import org.nuxeo.lib.stream.computation.StreamManager;
import org.nuxeo.lib.stream.log.LogManager;

/**
 * @since 9.3
 */
public interface StreamService {

    LogManager getLogManager();

    StreamManager getStreamManager();

    /**
     * Gets a {@link LogManager} corresponding to the config name. The service takes care of closing the manager on
     * shutdown you should not do it directly.
     *
     * @deprecated since 11.1 just use {@link #getLogManager()}.
     */
    @Deprecated
    LogManager getLogManager(String configName);


    /**
     * Gets a {@link StreamManager} that uses a LogManager matching the config name.
     *
     * @deprecated since 11.1 just use {@link #getStreamManager()}.
     */
    @Deprecated
    StreamManager getStreamManager(String configName);
}
