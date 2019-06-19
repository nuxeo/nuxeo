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
package org.nuxeo.ecm.platform.ui.web.directory;

import java.util.ArrayList;

import javax.faces.component.html.HtmlSelectManyListbox;
import javax.faces.view.facelets.TagAttribute;
import javax.faces.view.facelets.TagConfig;

/**
 * Legacy class for nxdir:selectOneListbox tag
 *
 * @since 6.0
 */
public class DirectorySelectManyListboxHandler extends DirectorySelectOneListboxHandler {

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
