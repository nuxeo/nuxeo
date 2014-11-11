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
 *     <a href="mailto:grenard@nuxeo.com">Guillaume</a>
 */
package org.nuxeo.ecm.platform.ui.web.renderer;

import java.io.IOException;
import java.util.Iterator;

import javax.faces.application.ResourceDependencies;
import javax.faces.application.ResourceDependency;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.model.SelectItem;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.ecm.platform.ui.web.util.ComponentUtils;

import com.sun.faces.renderkit.RenderKitUtils;
import com.sun.faces.renderkit.html_basic.SelectManyCheckboxListRenderer;

/**
 * @since 6.0
 */
@ResourceDependencies({
        @ResourceDependency(library = "org.richfaces", name = "jquery.js"),
        @ResourceDependency(library = "org.nuxeo", name = "widget-utils.js") })
public class NxSelectManyCheckboxListRenderer extends
        SelectManyCheckboxListRenderer {

    final String MORE_LESS_LIMIT_PROPERTY = "moreLessLimit";

    final String EMPTY_CHOICE_PROPERTY = "emptyChoiceMessage";

    public static final String RENDERER_TYPE = "org.nuxeo.NxSelectManyCheckboxList";

    @Override
    public void encodeEnd(FacesContext context, UIComponent component)
            throws IOException {

        super.encodeEnd(context, component);

        final String moreLessLimit = (String) component.getAttributes().get(
                MORE_LESS_LIMIT_PROPERTY);
        if (moreLessLimit != null) {
            ResponseWriter writer = context.getResponseWriter();
            final int moreLessLimitInt = Integer.parseInt(moreLessLimit);
            writer.startElement("a", component);
            writer.writeAttribute("href", "#", null);
            writer.writeAttribute(
                    "onclick",
                    "nuxeo.utils.moreLessTableRows('"
                            + component.getClientId() + "', true, "
                            + moreLessLimitInt + ");return false;", null);
            writer.writeAttribute("class", "nx-less-more-ctrl nx-more", null);
            writer.write(ComponentUtils.translate(context, "label.vocabulary.more"));
            writer.endElement("a");

            writer.startElement("a", component);
            writer.writeAttribute("href", "#", null);
            writer.writeAttribute(
                    "onclick",
                    "nuxeo.utils.moreLessTableRows('"
                            + component.getClientId() + "', false, "
                            + moreLessLimitInt + ");return false;", null);
            writer.writeAttribute("class", "nx-less-more-ctrl nx-less", null);
            writer.write(ComponentUtils.translate(context, "label.vocabulary.less"));
            writer.endElement("a");

            writer.startElement("script", component);
            writer.write("jQuery(document).ready(function(){nuxeo.utils.moreLessTableRows('"
                    + component.getClientId()
                    + "', false, "
                    + moreLessLimitInt
                    + ");});");
            writer.endElement("script");
        }

        Iterator<SelectItem> items =
                RenderKitUtils.getSelectItems(context, component);
        if (!items.hasNext()) {
            final String emptyChoiceMessage = (String) component.getAttributes().get(
                    EMPTY_CHOICE_PROPERTY);
            if (StringUtils.isNotBlank(emptyChoiceMessage)) {
                ResponseWriter writer = context.getResponseWriter();
                writer.startElement("div", component);
                writer.writeAttribute("class", "emptyResult", null);
                writer.write(ComponentUtils.translate(context, emptyChoiceMessage));
                writer.endElement("div");
            }
        }
    }

}
