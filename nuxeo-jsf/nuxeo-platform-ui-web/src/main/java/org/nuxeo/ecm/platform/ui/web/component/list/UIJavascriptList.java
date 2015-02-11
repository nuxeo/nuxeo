/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.platform.ui.web.component.list;

/**
 * Editable table component.
 * <p>
 * Allows to add/remove elements from an {@link UIJavascriptList}, inspired from Trinidad UIXCollection component.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public class UIJavascriptList extends UIEditableList {

    public static final String COMPONENT_TYPE = UIJavascriptList.class.getName();

    public static final String COMPONENT_FAMILY = UIJavascriptList.class.getName();

    @Override
    public String getFamily() {
        return COMPONENT_FAMILY;
    }

}
