/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 * $Id: TextWidgetTypeHandler.java 30416 2008-02-21 19:10:37Z atchertchian $
 */

package org.nuxeo.ecm.platform.forms.layout.facelets.plugins;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.component.UIComponent;
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

    public TextWidgetTypeHandler(TagConfig config) {
        super(config);
    }

    @Override
    public void apply(FaceletContext ctx, UIComponent parent, Widget widget) throws WidgetException, IOException {
        FaceletHandlerHelper helper = new FaceletHandlerHelper(tagConfig);
        String mode = widget.getMode();
        String widgetId = widget.getId();
        String widgetName = widget.getName();
        String widgetTagConfigId = widget.getTagConfigId();
        FaceletHandler leaf = getNextHandler(ctx, tagConfig, widget, null, helper);
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
            String msgId = FaceletHandlerHelper.generateMessageId(ctx, widgetName);
            ComponentHandler message = helper.getMessageComponentHandler(widgetTagConfigId, msgId, widgetId, null);
            FaceletHandler[] handlers = { input, message };
            FaceletHandler h = new CompositeFaceletHandler(handlers);
            h.apply(ctx, parent);
        } else {
            TagAttributes attributes = getViewTagAttributes(ctx, helper, widgetId, widget,
                    !BuiltinWidgetModes.isLikePlainMode(mode));
            // default on text for other modes
            ComponentHandler output = helper.getHtmlComponentHandler(widgetTagConfigId, attributes, leaf,
                    HtmlOutputText.COMPONENT_TYPE, null);
            if (BuiltinWidgetModes.PDF.equals(mode)) {
                // add a surrounding p:html tag handler
                FaceletHandler h = helper.getHtmlComponentHandler(widgetTagConfigId, new TagAttributesImpl(
                        new TagAttribute[0]), output, UIHtmlText.class.getName(), null);
                h.apply(ctx, parent);
            } else {
                output.apply(ctx, parent);
            }
        }
    }

    /**
     * Return tag attributes after having replaced the usual value expression for the 'value' field by a specific
     * expression to display the translated value if set as is in the widget properties.
     */
    protected TagAttributes getViewTagAttributes(FaceletContext ctx, FaceletHandlerHelper helper, String id,
            Widget widget, boolean addId) {
        List<TagAttribute> attrs = new ArrayList<>();
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
        Map<String, Serializable> widgetPropsClone = new HashMap<>();
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
