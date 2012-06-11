/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.runtime.service;

/**
 * Interface for services that need to track when the resources they hold have
 * been last modified.
 * <p>
 * This is useful to invalidate high-level cache when deploying components in
 * hot reload.
 *
 * @since 5.6
 */
public interface TimestampedService {

    /**
     * Returns the last modification timestamp.
     * <p>
     * This method is useful for third-party code implementing caching on
     * resources held by the component/service.
     *
     * @since 5.6
     * @see #setLastModified(Long)
     */
    Long getLastModified();

    /**
     * Sets the last modified date.
     * <p>
     * This method is supposed to be called whenever the component/service
     * resources or state changes.
     *
     * @since 5.6
     * @see #getLastModified()
     */
    void setLastModified(Long lastModified);

}
