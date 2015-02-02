/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Thomas Roger
 */

package org.nuxeo.ecm.platform.query.api;

/**
 * Listener to set on a {@link PageProvider} to be notified when the {@code PageProvider} changes.
 *
 * @since 5.7
 */
public interface PageProviderChangedListener {

    /**
     * Called when the current page of the {@code pageProvider} has changed.
     */
    void pageChanged(PageProvider<?> pageProvider);

    /**
     * Called when the {@code pageProvider} has refreshed.
     */
    void refreshed(PageProvider<?> pageProvider);

}
