/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.common.xmap.registry;

import org.nuxeo.common.xmap.Context;
import org.nuxeo.common.xmap.XAnnotatedObject;
import org.w3c.dom.Element;

/**
 * Interface for XML contributions management.
 *
 * @since 11.5
 */
public interface Registry {

    /**
     * Returns true if registry is just a placeholder and should not be used for registrations.
     */
    boolean isNull();

    /**
     * Initialized the registry.
     * <p>
     * Initialization can be performed when all registrations/unregistrations have been performed. This avoids
     * triggering unnecessary merge logics while the registry content is being modified.
     */
    void initialize();

    /**
     * Tags the registry with given identifier.
     * <p>
     * Several registrations can still be done with the same tag.
     */
    void tag(String id);

    /**
     * Returns true if registry has been tagged with given id.
     * <p>
     * After {@link #tag(String)} or {@link #register(Context, XAnnotatedObject, Element, String)} are called with given
     * tag, this method should return true.
     * <p>
     * After {@link #unregister(String)} is called with given tag, this method should return false.
     */
    boolean isTagged(String id);

    /**
     * Registers given element with given tag identifier.
     * <p>
     * Several registrations can be done with the same tag.
     */
    void register(Context ctx, XAnnotatedObject xObject, Element element, String tag);

    /**
     * Unregisters all elements previously registered with given tag identifier.
     */
    void unregister(String tag);

}
