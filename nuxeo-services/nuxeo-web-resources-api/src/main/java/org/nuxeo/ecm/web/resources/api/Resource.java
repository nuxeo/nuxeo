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
    public static final String PREFIX = "nuxeo:";

    String getName();

    String getType();

    String getPath();

    String getURI();

    void setURI(String uri);

    /**
     * Names of the resource dependencies.
     */
    List<String> getDependencies();

    /**
     * Names of the resource processors, hooking features like flavor replacement on the resource.
     */
    List<String> getProcessors();

    // TODO: check if this is still useful
    boolean isShrinkable();

}