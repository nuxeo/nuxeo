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
package org.nuxeo.ecm.platform.ui.web.directory;

import java.util.ArrayList;

import javax.faces.component.html.HtmlSelectManyListbox;
import javax.faces.view.facelets.TagAttribute;
import javax.faces.view.facelets.TagConfig;

/**
 * Legacy class for nxdir:selectOneListbox tag
 *
 * @since 5.9.6
 */
public class DirectorySelectManyListboxHandler extends
        DirectorySelectOneListboxHandler {

    public DirectorySelectManyListboxHandler(TagConfig config) {
        super(config);
    }

    @Override
    protected String getSelectComponentType() {
        return HtmlSelectManyListbox.COMPONENT_TYPE;
    }

    @Override
    protected void initAttributes(TagAttribute[] attrs) {
        super.initAttributes(attrs);
        // add hint for value conversion to collection
        select.add(getTagAttribute("collectionType", ArrayList.class.getName()));
    }

}
