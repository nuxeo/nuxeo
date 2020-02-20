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

import org.apache.commons.lang3.StringUtils;

/**
 * @since 7.3
 */
public enum ResourceType {

    any, unknown, css, js, bundle, html, jsfjs, jsfcss, xhtml, xhtmlfirst;

    public String getSuffix() {
        return "." + name();
    }

    /**
     * @since 7.4
     */
    public final boolean equals(String type) {
        return name().equalsIgnoreCase(type);
    }

    public final boolean matches(Resource r) {
        if (any == this) {
            return true;
        }
        if (r == null || r.getType() == null) {
            return true;
        }
        return equals(r.getType());
    }

    public static final ResourceType parse(String type) {
        for (ResourceType item : values()) {
            if (item.equals(type)) {
                return item;
            }
        }
        return unknown;
    }

    public static final boolean matches(String type, Resource r) {
        if (StringUtils.isBlank(type) || any.equals(type)) {
            return true;
        }
        String rt = r.getType();
        if (StringUtils.isBlank(rt)) {
            return true;
        }
        return type.equalsIgnoreCase(rt);
    }

}
