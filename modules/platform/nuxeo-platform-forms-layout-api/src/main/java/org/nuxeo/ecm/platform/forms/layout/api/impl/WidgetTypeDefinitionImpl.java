/*
 * (C) Copyright 2009-2010 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.platform.forms.layout.api.impl;

import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.platform.forms.layout.api.WidgetTypeConfiguration;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetTypeDefinition;

/**
 * Model for a widget type definition on client side.
 *
 * @author Anahide Tchertchian
 * @since 5.4
 */
public class WidgetTypeDefinitionImpl implements WidgetTypeDefinition {

    private static final long serialVersionUID = 1L;

    protected String name;

    /**
     * @since 6.0
     */
    protected List<String> aliases;

    protected String handlerClassName;

    protected Map<String, String> properties;

    protected WidgetTypeConfiguration configuration;

    // needed by GWT serialization
    public WidgetTypeDefinitionImpl() {
        super();
    }

    public WidgetTypeDefinitionImpl(String name, String handlerClassName, Map<String, String> properties,
            WidgetTypeConfiguration configuration) {
        super();
        this.name = name;
        this.handlerClassName = handlerClassName;
        this.properties = properties;
        this.configuration = configuration;
    }

    @Override
    public WidgetTypeConfiguration getConfiguration() {
        return configuration;
    }

    @Override
    public String getHandlerClassName() {
        return handlerClassName;
    }

    @Override
    public String getName() {
        return name;
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
        if (!(obj instanceof WidgetTypeDefinitionImpl)) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        WidgetTypeDefinitionImpl w = (WidgetTypeDefinitionImpl) obj;
        return new EqualsBuilder().append(name, w.name).append(aliases, w.aliases).append(handlerClassName,
                w.handlerClassName).append(properties, w.properties).append(configuration, w.configuration).isEquals();
    }

}