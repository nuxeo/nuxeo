/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

    public WidgetTypeImpl(String name, Class<?> typeClass,
            Map<String, String> properties) {
        this.name = name;
        this.typeClass = typeClass;
        this.properties = properties;
    }

    public String getName() {
        return name;
    }

    public Class<?> getWidgetTypeClass() {
        return typeClass;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public List<String> getAliases() {
        return aliases;
    }

    public void setAliases(List<String> aliases) {
        this.aliases = aliases;
    }

}
