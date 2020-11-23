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
package org.nuxeo.common.xmap;

import org.nuxeo.common.xmap.annotation.XNode;
import org.w3c.dom.Element;

/**
 * @since TODO
 */
public class XAnnotatedReference extends XAnnotatedMember {

    public XAnnotatedReference(XMap xmap, Class<?> type, String path, String fallbackPath, String defaultValue) {
        super(xmap, null);
        this.path = new Path(path);
        if (fallbackPath != null && !XNode.NO_FALLBACK_VALUE_MARKER.equals(fallbackPath)) {
            this.fallbackPath = new Path(fallbackPath);
        }
        trim = true;
        if (defaultValue != null && !XNode.NO_DEFAULT_VALUE_MARKER.equals(defaultValue)) {
            this.defaultValue = defaultValue;
        }
        this.type = type;
        valueFactory = xmap.getValueFactory(type);
        xao = xmap.register(type);
    }

    public XAnnotatedReference(XMap xmap, String path, String fallbackPath, boolean defaultValue) {
        this(xmap, Boolean.class, path, fallbackPath, String.valueOf(defaultValue));
    }

    @Override
    protected void setValue(Object instance, Object value) {
        // NOOP
    }

    @Override
    public void toXML(Object instance, Element parent) {
        // NOOP
    }

}
