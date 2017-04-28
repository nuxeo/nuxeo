/*
 * (C) Copyright 2006-2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thierry Delprat
 */
package org.nuxeo.ecm.platform.audit.service;

import org.nuxeo.ecm.platform.audit.api.Logs;

/**
 * Audit Backend SPI
 *
 * @author tiry
 */
public interface AuditBackend extends Logs {

    int getApplicationStartedOrder();

    void onApplicationStarted();

    /**
     * @since 9.2 with default backward compatibility by delegating to deprecated API {@link #onShutdown()}
     */
    default void onApplicationStopped() {
        onShutdown();
    }

    /**
     * @deprecated since 9.2 replaced with {@link #onStandby()}
     */
    @Deprecated
    default void onShutdown() {
        throw new UnsupportedOperationException("deprecated API, should not be invoked");
    }


}
