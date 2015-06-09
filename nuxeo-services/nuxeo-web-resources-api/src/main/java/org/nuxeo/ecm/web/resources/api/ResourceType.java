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

import org.apache.commons.lang.StringUtils;

/**
 * @since 7.3
 */
public enum ResourceType {

    any, unknown, css, js, bundle, html;

    public String getSuffix() {
        return "." + name();
    }

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
        if (r == null || r.getType() == null) {
            return true;
        }
        if (this.name().toLowerCase().equals(r.getType().toLowerCase())) {
            return true;
        }
        return false;
    }

    public static final boolean matches(String type, Resource r) {
        if (StringUtils.isBlank(type) || ResourceType.any.name().equals(type.toLowerCase())) {
            return true;
        }
        String rt = r.getType();
        if (StringUtils.isBlank(rt)) {
            return true;
        }
        if (type.toLowerCase().equals(rt.toLowerCase())) {
            return true;
        }
        return false;
    }

}
