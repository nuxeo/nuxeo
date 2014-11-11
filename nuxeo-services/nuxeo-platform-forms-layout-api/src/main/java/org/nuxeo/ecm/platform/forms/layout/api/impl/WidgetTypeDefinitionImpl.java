/*
 * (C) Copyright 2009-2010 Nuxeo SA (http://nuxeo.com/).
 * This is unpublished proprietary source code of Nuxeo SA. All rights reserved.
 * Notice of copyright on this source code does not indicate publication.
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

    public WidgetTypeDefinitionImpl(String name, String handlerClassName,
            Map<String, String> properties,
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

}
