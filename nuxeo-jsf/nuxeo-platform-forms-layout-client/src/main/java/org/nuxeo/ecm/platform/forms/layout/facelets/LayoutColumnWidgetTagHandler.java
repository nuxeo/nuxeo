/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.platform.forms.layout.facelets;

import javax.faces.view.facelets.TagConfig;

/**
 * Layout widget recursion tag handler.
 * <p>
 * Iterates over a layout column widgets and apply next handlers as many times as needed.
 * <p>
 * Only works when used inside a tag using the {@link LayoutColumnTagHandler}.
 *
 * @since 8.2
 */
public class LayoutColumnWidgetTagHandler extends LayoutRowWidgetTagHandler {

    public LayoutColumnWidgetTagHandler(TagConfig config) {
        super(config);
    }

    protected String getInstanceName() {
        return RenderVariables.columnVariables.layoutColumn.name();
    }

}
