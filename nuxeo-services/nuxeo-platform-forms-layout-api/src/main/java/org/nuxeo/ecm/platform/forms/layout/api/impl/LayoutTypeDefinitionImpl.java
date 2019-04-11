/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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

import org.nuxeo.ecm.platform.forms.layout.api.LayoutTypeConfiguration;
import org.nuxeo.ecm.platform.forms.layout.api.LayoutTypeDefinition;

/**
 * @since 6.0
 */
public class LayoutTypeDefinitionImpl implements LayoutTypeDefinition {

    private static final long serialVersionUID = 1L;

    protected String name;

    protected List<String> aliases;

    protected Map<String, String> templates;

    protected LayoutTypeConfiguration configuration;

    // needed by GWT serialization
    public LayoutTypeDefinitionImpl() {
        super();
    }

    public LayoutTypeDefinitionImpl(String name, Map<String, String> templates, LayoutTypeConfiguration configuration) {
        super();
        this.name = name;
        this.templates = templates;
        this.configuration = configuration;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Map<String, String> getTemplates() {
        return templates;
    }

    @Override
    public String getTemplate(String mode) {
        return LayoutDefinitionImpl.getTemplate(templates, mode);
    }

    @Override
    public LayoutTypeConfiguration getConfiguration() {
        return configuration;
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
        if (!(obj instanceof LayoutTypeDefinitionImpl)) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        LayoutTypeDefinitionImpl lt = (LayoutTypeDefinitionImpl) obj;
        return new EqualsBuilder().append(name, lt.name).append(aliases, lt.aliases).append(templates, lt.templates).append(
                configuration, lt.configuration).isEquals();
    }

}
