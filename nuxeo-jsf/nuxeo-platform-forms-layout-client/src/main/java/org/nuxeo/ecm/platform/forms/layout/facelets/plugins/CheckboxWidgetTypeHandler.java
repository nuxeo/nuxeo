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
 *     btatar
 *     Anahide Tchertchian
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.forms.layout.facelets.plugins;

import java.util.ArrayList;
import java.util.List;

import javax.faces.component.html.HtmlOutputText;
import javax.faces.component.html.HtmlSelectBooleanCheckbox;

import org.nuxeo.ecm.platform.forms.layout.api.BuiltinWidgetModes;
import org.nuxeo.ecm.platform.forms.layout.api.FieldDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.Widget;
import org.nuxeo.ecm.platform.forms.layout.api.exceptions.WidgetException;
import org.nuxeo.ecm.platform.forms.layout.facelets.FaceletHandlerHelper;
import org.nuxeo.ecm.platform.forms.layout.facelets.LeafFaceletHandler;
import org.nuxeo.ecm.platform.forms.layout.facelets.ValueExpressionHelper;
import org.nuxeo.ecm.platform.ui.web.component.seam.UIHtmlText;
import org.nuxeo.ecm.platform.ui.web.renderer.NXCheckboxRenderer;

import com.sun.facelets.FaceletContext;
import com.sun.facelets.FaceletHandler;
import com.sun.facelets.tag.CompositeFaceletHandler;
import com.sun.facelets.tag.TagAttribute;
import com.sun.facelets.tag.TagAttributes;
import com.sun.facelets.tag.TagConfig;
import com.sun.facelets.tag.jsf.ComponentHandler;

/**
 * Checkbox widget that generates a checkbox for a boolean value in edit mode,
 * and displays the boolean value in view mode.
 * <p>
 * In view mode, it expects the messages 'label.yes' and 'label.no' to be
 * present in a bundle called 'messages' for internationalization, when the
 * bound value is computed from field definitions.
 */
public class CheckboxWidgetTypeHandler extends AbstractWidgetTypeHandler {

    private static final long serialVersionUID = 1L;

    @Override
    public FaceletHandler getFaceletHandler(FaceletContext ctx,
            TagConfig tagConfig, Widget widget, FaceletHandler[] subHandlers)
            throws WidgetException {
        FaceletHandlerHelper helper = new FaceletHandlerHelper(ctx, tagConfig);
        String mode = widget.getMode();
        String widgetId = widget.getId();
        String widgetName = widget.getName();
        FaceletHandler leaf = new LeafFaceletHandler();
        if (BuiltinWidgetModes.EDIT.equals(mode)) {
            TagAttributes attributes = helper.getTagAttributes(widgetId, widget);
            ComponentHandler input = helper.getHtmlComponentHandler(attributes,
                    leaf, HtmlSelectBooleanCheckbox.COMPONENT_TYPE,
                    NXCheckboxRenderer.RENDERER_TYPE);
            String msgId = helper.generateMessageId(widgetName);
            ComponentHandler message = helper.getMessageComponentHandler(msgId,
                    widgetId, null);
            FaceletHandler[] handlers = { input, message };
            return new CompositeFaceletHandler(handlers);
        } else {
            TagAttributes attributes = getViewTagAttributes(ctx, helper,
                    widgetId, widget, !BuiltinWidgetModes.isLikePlainMode(mode));
            // default on text for other modes
            ComponentHandler output = helper.getHtmlComponentHandler(
                    attributes, leaf, HtmlOutputText.COMPONENT_TYPE, null);
            if (BuiltinWidgetModes.PDF.equals(mode)) {
                // add a surrounding p:html tag handler
                return helper.getHtmlComponentHandler(new TagAttributes(
                        new TagAttribute[0]), output,
                        UIHtmlText.class.getName(), null);
            } else {
                return output;
            }
        }
    }

    /**
     * Return tag attributes after having replaced the usual value expression
     * for the 'value' field by a specific expression to display the boolean
     * value as an internationalized label.
     */
    protected TagAttributes getViewTagAttributes(FaceletContext ctx,
            FaceletHandlerHelper helper, String id, Widget widget, boolean addId) {
        List<TagAttribute> attrs = new ArrayList<TagAttribute>();
        FieldDefinition[] fields = widget.getFieldDefinitions();
        if (fields != null && fields.length > 0) {
            FieldDefinition field = fields[0];
            String bareExpression = ValueExpressionHelper.createBareExpressionString(
                    widget.getValueName(), field);
            String bundleName = ctx.getFacesContext().getApplication().getMessageBundle();
            String messageYes = String.format("%s['label.yes']", bundleName);
            String messageNo = String.format("%s['label.no']", bundleName);
            String expression = String.format("#{%s ? %s : %s}",
                    bareExpression, messageYes, messageNo);
            TagAttribute valueAttr = helper.createAttribute("value", expression);
            attrs.add(valueAttr);
        }
        // fill with widget properties
        List<TagAttribute> propertyAttrs = helper.getTagAttributes(
                widget.getProperties(), true);
        if (propertyAttrs != null) {
            attrs.addAll(propertyAttrs);
        }
        TagAttributes widgetAttrs = FaceletHandlerHelper.getTagAttributes(attrs);
        // handle id
        if (!addId) {
            return widgetAttrs;
        } else {
            return FaceletHandlerHelper.addTagAttribute(widgetAttrs,
                    helper.createAttribute("id", id));
        }

    }
}
