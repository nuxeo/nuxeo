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
 *     <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 * $Id: TextWidgetTypeHandler.java 30416 2008-02-21 19:10:37Z atchertchian $
 */

package org.nuxeo.ecm.platform.forms.layout.facelets.plugins;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.component.html.HtmlInputText;
import javax.faces.component.html.HtmlOutputText;
import javax.faces.view.facelets.ComponentHandler;
import javax.faces.view.facelets.CompositeFaceletHandler;
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.FaceletHandler;
import javax.faces.view.facelets.TagAttribute;
import javax.faces.view.facelets.TagAttributes;
import javax.faces.view.facelets.TagConfig;

import org.nuxeo.ecm.platform.forms.layout.api.BuiltinWidgetModes;
import org.nuxeo.ecm.platform.forms.layout.api.FieldDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.Widget;
import org.nuxeo.ecm.platform.forms.layout.api.exceptions.WidgetException;
import org.nuxeo.ecm.platform.forms.layout.facelets.FaceletHandlerHelper;
import org.nuxeo.ecm.platform.forms.layout.facelets.ValueExpressionHelper;
import org.nuxeo.ecm.platform.ui.web.component.seam.UIHtmlText;
import org.nuxeo.ecm.platform.ui.web.util.ComponentTagUtils;

import com.sun.faces.facelets.tag.TagAttributesImpl;

/**
 * Text widget.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public class TextWidgetTypeHandler extends AbstractWidgetTypeHandler {

    private static final long serialVersionUID = 1495841177711755669L;

    @Override
    public FaceletHandler getFaceletHandler(FaceletContext ctx, TagConfig tagConfig, Widget widget,
            FaceletHandler[] subHandlers) throws WidgetException {
        FaceletHandlerHelper helper = new FaceletHandlerHelper(ctx, tagConfig);
        String mode = widget.getMode();
        String widgetId = widget.getId();
        String widgetName = widget.getName();
        String widgetTagConfigId = widget.getTagConfigId();
        FaceletHandler leaf = getNextHandler(ctx, tagConfig, widget, subHandlers, helper);
        if (BuiltinWidgetModes.EDIT.equals(mode)) {
            TagAttributes attributes = helper.getTagAttributes(widgetId, widget);
            // Make text fields automatically switch to right-to-left if
            // not told otherwise
            if (widget.getProperty(FaceletHandlerHelper.DIR_PROPERTY) == null) {
                TagAttribute dir = helper.createAttribute(FaceletHandlerHelper.DIR_PROPERTY,
                        FaceletHandlerHelper.DIR_AUTO);
                attributes = FaceletHandlerHelper.addTagAttribute(attributes, dir);
            }
            ComponentHandler input = helper.getHtmlComponentHandler(widgetTagConfigId, attributes, leaf,
                    HtmlInputText.COMPONENT_TYPE, null);
            String msgId = helper.generateMessageId(widgetName);
            ComponentHandler message = helper.getMessageComponentHandler(widgetTagConfigId, msgId, widgetId, null);
            FaceletHandler[] handlers = { input, message };
            return new CompositeFaceletHandler(handlers);
        } else {
            TagAttributes attributes = getViewTagAttributes(ctx, helper, widgetId, widget,
                    !BuiltinWidgetModes.isLikePlainMode(mode));
            // default on text for other modes
            ComponentHandler output = helper.getHtmlComponentHandler(widgetTagConfigId, attributes, leaf,
                    HtmlOutputText.COMPONENT_TYPE, null);
            if (BuiltinWidgetModes.PDF.equals(mode)) {
                // add a surrounding p:html tag handler
                return helper.getHtmlComponentHandler(widgetTagConfigId, new TagAttributesImpl(new TagAttribute[0]),
                        output, UIHtmlText.class.getName(), null);
            } else {
                return output;
            }
        }
    }

    /**
     * Return tag attributes after having replaced the usual value expression for the 'value' field by a specific
     * expression to display the translated value if set as is in the widget properties.
     */
    protected TagAttributes getViewTagAttributes(FaceletContext ctx, FaceletHandlerHelper helper, String id,
            Widget widget, boolean addId) {
        List<TagAttribute> attrs = new ArrayList<TagAttribute>();
        FieldDefinition[] fields = widget.getFieldDefinitions();
        if (fields != null && fields.length > 0) {
            FieldDefinition field = fields[0];
            String pname = field != null ? field.getPropertyName() : null;
            if (ComponentTagUtils.isValueReference(pname)) {
                // do not override value for localization in this case, see NXP-13456
                TagAttribute valueAttr = helper.createAttribute("value",
                        ValueExpressionHelper.createExpressionString(widget.getValueName(), field));
                attrs.add(valueAttr);
            } else {
                String bareExpression = ValueExpressionHelper.createBareExpressionString(widget.getValueName(), field);
                String bundleName = ctx.getFacesContext().getApplication().getMessageBundle();
                String localizedExpression = bundleName + "[" + bareExpression + "]";
                String expression = "#{widget.properties.localize ? " + localizedExpression + " : " + bareExpression
                        + "}";
                TagAttribute valueAttr = helper.createAttribute("value", expression);
                attrs.add(valueAttr);
            }
        }

        // fill with widget properties
        Map<String, Serializable> widgetPropsClone = new HashMap<String, Serializable>();
        Map<String, Serializable> widgetProps = widget.getProperties();
        if (widgetProps != null) {
            widgetPropsClone.putAll(widgetProps);
            // remove localize property
            widgetPropsClone.remove("localize");
        }
        List<TagAttribute> propertyAttrs = helper.getTagAttributes(widgetPropsClone, null, true, widget.getType(),
                widget.getTypeCategory(), widget.getMode());
        if (propertyAttrs != null) {
            attrs.addAll(propertyAttrs);
        }
        TagAttributes widgetAttrs = FaceletHandlerHelper.getTagAttributes(attrs);
        // handle id
        if (!addId) {
            return widgetAttrs;
        } else {
            TagAttributes res = FaceletHandlerHelper.addTagAttribute(widgetAttrs, helper.createAttribute("id", id));
            // Make text fields automatically switch to right-to-left if
            // not told otherwise
            if (widget.getProperty(FaceletHandlerHelper.DIR_PROPERTY) == null) {
                TagAttribute dir = helper.createAttribute(FaceletHandlerHelper.DIR_PROPERTY,
                        FaceletHandlerHelper.DIR_AUTO);
                res = FaceletHandlerHelper.addTagAttribute(res, dir);
            }
            return res;
        }

    }

}
