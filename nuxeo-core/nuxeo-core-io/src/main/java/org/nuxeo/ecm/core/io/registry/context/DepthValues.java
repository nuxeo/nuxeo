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
 *     Nicolas Chapurlat <nchapurlat@nuxeo.com>
 */

package org.nuxeo.ecm.core.io.registry.context;

/**
 * Possible depth values for "marshaller-to-marshaller" calls (to avoid the infinite loops).
 *
 * @since 7.2
 */
public enum DepthValues {

    /**
     * Loads / Fetches / Enriches the first level element(s).
     */
    root,

    /**
     * Loads / Fetches / Enriches the first level element(s) and its (their) children.
     */
    children,

    /**
     * Loads / Fetches / Enriches the first level element(s), its (their) children, and the grandchildren.
     */
    max;

    /**
     * Gets the corresponding depth value.
     *
     * @return The depth.
     * @since 7.2
     */
    public int getDepth() {
        return ordinal() + 1;
    }

}
