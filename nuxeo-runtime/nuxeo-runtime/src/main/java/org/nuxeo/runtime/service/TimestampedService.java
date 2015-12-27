/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.runtime.service;

/**
 * Interface for services that need to track when the resources they hold have been last modified.
 * <p>
 * This is useful to invalidate high-level cache when deploying components in hot reload.
 *
 * @since 5.6
 */
public interface TimestampedService {

    /**
     * Returns the last modification timestamp or null if unknown.
     * <p>
     * This method is useful for third-party code implementing caching on resources held by the component/service.
     *
     * @since 5.6
     * @see #setLastModified(Long)
     */
    Long getLastModified();

    /**
     * Sets the last modified date.
     * <p>
     * This method is supposed to be called whenever the component/service resources or state changes.
     *
     * @since 5.6
     * @see #getLastModified()
     */
    void setLastModified(Long lastModified);

}
