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

/**
 * @since 7.3
 */
public enum ResourceType {

    any, unknown, css, js, bundle;

    public static final ResourceType parse(String type) {
        for (ResourceType item : values()) {
            if (item.name().equals(type)) {
                return item;
            }
        }
        return ResourceType.unknown;
    }

    public final boolean matches(Resource r) {
        if (ResourceType.any == this) {
            return true;
        }
        if (r.getType() == ResourceType.any) {
            return true;
        }
        if (r.getType() == this) {
            return true;
        }
        return false;
    }

}
