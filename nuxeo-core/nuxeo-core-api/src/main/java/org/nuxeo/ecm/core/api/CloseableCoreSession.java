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
 */
public interface CloseableCoreSession extends CoreSession, AutoCloseable {

    /**
     * Closes this session.
     *
     * @since 5.9.3
     */
    @Override
    void close();

    /**
     * Destroys any system resources held by this instance.
     * <p>
     * Called when the instance is no more needed.
     */
    void destroy();

}
