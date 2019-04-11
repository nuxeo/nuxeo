/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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

package org.nuxeo.ecm.web.resources.api;

import java.io.Serializable;
import java.util.List;

/**
 * Typed Web resource (js, css, bundle).
 *
 * @since 7.3
 */
public interface Resource extends Serializable {

    /**
     * Marker for Nuxeo web resources, used by URI locators.
     */
    String PREFIX = "nuxeo:";

    String getName();

    String getType();

    String getPath();

    String getURI();

    /**
     * Returns an optional target to push resources to in the page.
     * <p>
     * Currently only useful to JSF resources.
     *
     * @since 7.10
     */
    String getTarget();

    /**
     * Names of the resource dependencies.
     */
    List<String> getDependencies();

    /**
     * Names of the resource processors, hooking features like flavor replacement on the resource.
     */
    List<String> getProcessors();

    /**
     * Returns true if resource can be minimized.
     * <p>
     * Returns true by default if not specified.
     */
    boolean isShrinkable();

}
