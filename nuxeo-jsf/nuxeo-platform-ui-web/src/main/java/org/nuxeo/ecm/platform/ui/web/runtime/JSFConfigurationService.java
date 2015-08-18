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
 *      Andre Justo
 */
package org.nuxeo.ecm.platform.ui.web.runtime;

/**
 * Service to hold JSF runtime configuration properties.
 *
 * @since 7.4
 */
public interface JSFConfigurationService {

    /**
     * Gets the given property value if any, otherwise null.
     *
     * @param key the property key
     * @return the property value if any or null otherwise
     */
    String getProperty(String key);

    /**
     * Returns true if given property is true when compared to a boolean value.
     */
    boolean isBooleanPropertyTrue(String key);

    /**
     * Returns true if given property is false when compared to a boolean value.
     */
    boolean isBooleanPropertyFalse(String key);
}
