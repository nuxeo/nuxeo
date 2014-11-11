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
package org.nuxeo.ecm.platform.forms.layout.api.impl;

import org.nuxeo.ecm.platform.forms.layout.api.WidgetReference;

/**
 * @since 5.5
 */
public class WidgetReferenceImpl implements WidgetReference {

    private static final long serialVersionUID = 1L;

    protected String category;

    protected String name;

    // needed by GWT serialization
    protected WidgetReferenceImpl() {
        super();
    }

    public WidgetReferenceImpl(String widget) {
        this(null, widget);
    }

    public WidgetReferenceImpl(String category, String name) {
        super();
        this.category = category;
        this.name = name;
    }

    @Override
    public String getCategory() {
        return category;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public WidgetReference clone() {
        return new WidgetReferenceImpl(category, name);
    }

}
