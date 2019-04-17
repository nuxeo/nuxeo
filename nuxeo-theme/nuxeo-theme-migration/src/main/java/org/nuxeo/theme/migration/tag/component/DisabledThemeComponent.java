/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.theme.migration.tag.component;

import java.io.IOException;

import javax.faces.component.UIComponentBase;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

/**
 * Disabled component for theme compat.
 *
 * @since 7.4
 */
public class DisabledThemeComponent extends UIComponentBase {

    public static final String COMPONENT_TYPE = "nuxeo.web.theme.disabled";

    public static final String COMPONENT_FAMILY = "nuxeo.web.theme.disabled";

    @Override
    public String getFamily() {
        return COMPONENT_FAMILY;
    }

    @Override
    public String getRendererType() {
        return null;
    }

    @Override
    public void encodeBegin(FacesContext context) throws IOException {
        @SuppressWarnings("resource")
        ResponseWriter writer = context.getResponseWriter();
        writer.write("This theme tag is disabled. Please use another tag library");
        writer.flush();
    }

}
