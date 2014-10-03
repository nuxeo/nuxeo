/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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
package org.nuxeo.ecm.platform.forms.layout.api.impl;

import java.util.Map;

import org.nuxeo.ecm.platform.forms.layout.api.LayoutTypeConfiguration;
import org.nuxeo.ecm.platform.forms.layout.api.LayoutTypeDefinition;

/**
 * @since 5.9.6
 */
public class LayoutTypeDefinitionImpl implements LayoutTypeDefinition {

    private static final long serialVersionUID = 1L;

    protected String name;

    protected Map<String, String> templates;

    protected LayoutTypeConfiguration configuration;

    // needed by GWT serialization
    public LayoutTypeDefinitionImpl() {
        super();
    }

    public LayoutTypeDefinitionImpl(String name, Map<String, String> templates,
            LayoutTypeConfiguration configuration) {
        super();
        this.name = name;
        this.templates = templates;
        this.configuration = configuration;
    }

    @Override
    public String getName() {
        return name;
    }

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

}
