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
 *     <a href="mailto:grenard@nuxeo.com">Guillaume</a>
 */
package org.nuxeo.ecm.platform.ui.web.renderer;

import java.io.IOException;
import java.util.Iterator;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.model.SelectItem;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.ecm.platform.ui.web.util.ComponentUtils;

import com.sun.faces.renderkit.RenderKitUtils;
import com.sun.faces.renderkit.html_basic.SelectManyCheckboxListRenderer;

/**
 * @since 6.0
 */
public class NxSelectManyCheckboxListRenderer extends SelectManyCheckboxListRenderer {

    final String MORE_LESS_LIMIT_PROPERTY = "moreLessLimit";

    final String EMPTY_CHOICE_PROPERTY = "emptyChoiceMessage";

    public static final String RENDERER_TYPE = "org.nuxeo.NxSelectManyCheckboxList";

    @Override
    public void encodeEnd(FacesContext context, UIComponent component) throws IOException {

        super.encodeEnd(context, component);

        final String moreLessLimit = (String) component.getAttributes().get(MORE_LESS_LIMIT_PROPERTY);
        if (moreLessLimit != null) {
            @SuppressWarnings("resource")
            ResponseWriter writer = context.getResponseWriter();
            final int moreLessLimitInt = Integer.parseInt(moreLessLimit);
            writer.startElement("a", component);
            writer.writeAttribute("href", "#", null);
            writer.writeAttribute("onclick", "nuxeo.utils.moreLessTableRows('" + component.getClientId() + "', true, "
                    + moreLessLimitInt + ");return false;", null);
            writer.writeAttribute("class", "nx-less-more-ctrl nx-more", null);
            writer.write(ComponentUtils.translate(context, "label.vocabulary.more"));
            writer.endElement("a");

            writer.startElement("a", component);
            writer.writeAttribute("href", "#", null);
            writer.writeAttribute("onclick", "nuxeo.utils.moreLessTableRows('" + component.getClientId() + "', false, "
                    + moreLessLimitInt + ");return false;", null);
            writer.writeAttribute("class", "nx-less-more-ctrl nx-less", null);
            writer.write(ComponentUtils.translate(context, "label.vocabulary.less"));
            writer.endElement("a");

            writer.startElement("script", component);
            writer.write("jQuery(document).ready(function(){nuxeo.utils.moreLessTableRows('" + component.getClientId()
                    + "', false, " + moreLessLimitInt + ");});");
            writer.endElement("script");
        }

        Iterator<SelectItem> items = RenderKitUtils.getSelectItems(context, component);
        if (!items.hasNext()) {
            final String emptyChoiceMessage = (String) component.getAttributes().get(EMPTY_CHOICE_PROPERTY);
            if (StringUtils.isNotBlank(emptyChoiceMessage)) {
                @SuppressWarnings("resource")
                ResponseWriter writer = context.getResponseWriter();
                writer.startElement("div", component);
                writer.writeAttribute("class", "emptyResult", null);
                writer.write(ComponentUtils.translate(context, emptyChoiceMessage));
                writer.endElement("div");
            }
        }
    }

}
