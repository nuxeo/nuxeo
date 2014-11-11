/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.forms.layout.api.converters;

import java.io.Serializable;
import java.util.Map;

/**
 * Context propagated to all layout and widget converters.
 * <p>
 * Holds the language (locale) and can also hold a map of serializable objects.
 *
 * @since 5.5
 */
public class LayoutConversionContext {

    protected String language;

    protected Map<String, Serializable> properties;

    public LayoutConversionContext(String language,
            Map<String, Serializable> properties) {
        super();
        this.language = language;
        this.properties = properties;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public Map<String, Serializable> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Serializable> properties) {
        this.properties = properties;
    }

}
