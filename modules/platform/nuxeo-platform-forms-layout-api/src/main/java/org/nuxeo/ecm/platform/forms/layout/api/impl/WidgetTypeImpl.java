/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 * $Id: WidgetTypeImpl.java 28478 2008-01-04 12:53:58Z sfermigier $
 */

package org.nuxeo.ecm.platform.forms.layout.api.impl;

import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.platform.forms.layout.api.WidgetType;

/**
 * Implementation for widget types.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public class WidgetTypeImpl implements WidgetType {

    private static final long serialVersionUID = -6449946287266106594L;

    protected String name;

    protected List<String> aliases;

    protected Class<?> typeClass;

    protected Map<String, String> properties;

    // needed by GWT serialization
    protected WidgetTypeImpl() {
        super();
    }

    public WidgetTypeImpl(String name, Class<?> typeClass, Map<String, String> properties) {
        this.name = name;
        this.typeClass = typeClass;
        this.properties = properties;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Class<?> getWidgetTypeClass() {
        return typeClass;
    }

    @Override
    public Map<String, String> getProperties() {
        return properties;
    }

    @Override
    public List<String> getAliases() {
        return aliases;
    }

    public void setAliases(List<String> aliases) {
        this.aliases = aliases;
    }

    /**
     * @since 7.2
     */
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof WidgetTypeImpl)) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        WidgetTypeImpl w = (WidgetTypeImpl) obj;
        return new EqualsBuilder().append(name, w.name).append(aliases, w.aliases).append(typeClass, w.typeClass).append(
                properties, w.properties).isEquals();
    }

}
