/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
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
