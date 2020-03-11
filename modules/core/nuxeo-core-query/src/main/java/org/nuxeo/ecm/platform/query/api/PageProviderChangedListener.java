/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
