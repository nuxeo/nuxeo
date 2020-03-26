/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.api;

/**
 * Closeable Core Session.
 *
 * @since 10.1
 * @deprecated since 11.1, use just {@link CoreSession} instead
 */
@Deprecated
public interface CloseableCoreSession extends CoreSession, AutoCloseable {

    /**
     * Does nothing.
     *
     * @since 5.9.3
     * @deprecated since 11.1
     */
    @Override
    void close();

    /**
     * Does nothing.
     *
     * @deprecated since 11.1
     */
    void destroy();

}
