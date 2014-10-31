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
package org.nuxeo.ecm.platform.forms.layout.export;

import java.util.ArrayList;
import java.util.Collection;

import org.nuxeo.ecm.platform.forms.layout.api.LayoutTypeDefinition;

/**
 * Helper class representing a list of layout type definitions.
 *
 * @since 6.0
 */
public class LayoutTypeDefinitions extends ArrayList<LayoutTypeDefinition> {

    private static final long serialVersionUID = 1L;

    public LayoutTypeDefinitions() {
        super();
    }

    public LayoutTypeDefinitions(Collection<? extends LayoutTypeDefinition> c) {
        super(c);
    }

}
