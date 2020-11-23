/*
 * (C) Copyright 2006-2020 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *     Bogdan Stefanescu
 *     Anahide Tchertchian
 */

package org.nuxeo.runtime.model;

import org.nuxeo.common.xmap.XMap;

/**
 * A component extension point.
 * <p>
 * Extension points are described by a name and a list of optional contribution object classes.
 * <p>
 * When defined, the contribution object classes are the type of objects accepted by this extension point.
 * <p>
 * The extension point is also responsible for extracting contribution objects from the extension data, if any.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public interface ExtensionPoint {

    /**
     * Gets the extension point name.
     *
     * @return the extension point name
     */
    String getName();

    /**
     * Gets the object types of the contributions accepted by this extension point.
     *
     * @return the accepted contribution types
     */
    Class<?>[] getContributions();

    /**
     * Gets the comment attached to this extension point if any.
     *
     * @return the comment
     */
    String getDocumentation();

    /**
     * Get the component owning the base extension which this one extends.
     * <p>
     * If this method does not return null, it means the current extension point is extending another extension point
     * and should forward any contribution to the base extension. The base extension has the same name as this one but
     * it is declared in another component.
     *
     * @return the base extension point if this extension point is extending another extension point, or null if none
     */
    String getSuperComponent();

    /**
     * Returns the potential custom registry class for this extension point.
     *
     * @since 11.5
     */
    String getRegistryClass();

    /**
     * Returns the XMap object that matches {@link #getContributions()} classes.
     *
     * @since 11.5
     */
    XMap getXMap();

}
