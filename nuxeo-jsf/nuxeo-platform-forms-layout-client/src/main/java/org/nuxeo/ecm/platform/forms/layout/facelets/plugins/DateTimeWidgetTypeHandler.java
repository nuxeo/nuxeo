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
 * $Id: DateTimeWidgetTypeHandler.java 30416 2008-02-21 19:10:37Z atchertchian $
 */

package org.nuxeo.ecm.platform.forms.layout.facelets.plugins;

import java.io.IOException;

import javax.faces.component.UIComponent;
import javax.faces.component.html.HtmlOutputText;
import javax.faces.component.html.HtmlPanelGroup;
import javax.faces.view.facelets.ComponentConfig;
import javax.faces.view.facelets.ComponentHandler;
import javax.faces.view.facelets.CompositeFaceletHandler;
import javax.faces.view.facelets.ConverterConfig;
import javax.faces.view.facelets.ConverterHandler;
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.FaceletHandler;
import javax.faces.view.facelets.TagAttribute;
import javax.faces.view.facelets.TagAttributes;
import javax.faces.view.facelets.TagConfig;
import javax.faces.view.facelets.ValidatorConfig;
import javax.faces.view.facelets.ValidatorHandler;

import org.nuxeo.ecm.platform.forms.layout.api.BuiltinWidgetModes;
import org.nuxeo.ecm.platform.forms.layout.api.Widget;
import org.nuxeo.ecm.platform.forms.layout.api.exceptions.WidgetException;
import org.nuxeo.ecm.platform.forms.layout.facelets.FaceletHandlerHelper;
import org.nuxeo.ecm.platform.ui.web.component.seam.UIHtmlText;
import org.nuxeo.ecm.platform.ui.web.tag.handler.InputDateTimeTagHandler;
import org.nuxeo.ecm.platform.ui.web.tag.handler.LeafFaceletHandler;
import org.nuxeo.ecm.platform.ui.web.tag.handler.TagConfigFactory;
import org.richfaces.component.UICalendar;

import com.sun.faces.facelets.tag.TagAttributesImpl;
import com.sun.faces.facelets.tag.jsf.core.ConvertDateTimeHandler;

/**
 * Date time widget
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public class DateTimeWidgetTypeHandler extends AbstractWidgetTypeHandler {

    public DateTimeWidgetTypeHandler(TagConfig config) {
        super(config);
    }

    @Override
    public void apply(FaceletContext ctx, UIComponent parent, Widget widget) throws WidgetException, IOException {
        FaceletHandlerHelper helper = new FaceletHandlerHelper(tagConfig);
        String mode = widget.getMode();
        String widgetId = widget.getId();
        String widgetName = widget.getName();
        String widgetTagConfigId = widget.getTagConfigId();
        TagAttributes attributes;
        if (BuiltinWidgetModes.isLikePlainMode(mode)) {
            // use attributes without id
            attributes = helper.getTagAttributes(widget);
        } else {
            attributes = helper.getTagAttributes(widgetId, widget);
        }
        FaceletHandler leaf = getNextHandler(ctx, tagConfig, widget, null, helper);
        if (BuiltinWidgetModes.EDIT.equals(mode)) {
            ValidatorConfig validatorConfig = TagConfigFactory.createValidatorConfig(tagConfig,
                    widget.getTagConfigId(), FaceletHandlerHelper.getTagAttributes(), new LeafFaceletHandler(),
                    org.nuxeo.ecm.platform.ui.web.component.date.DateTimeValidator.VALIDATOR_ID);
            ValidatorHandler validateHandler = new ValidatorHandler(validatorConfig);
            FaceletHandler nextHandler = new CompositeFaceletHandler(new FaceletHandler[] { validateHandler, leaf });
            ComponentConfig config = TagConfigFactory.createComponentConfig(tagConfig, widget.getTagConfigId(),
                    attributes, nextHandler, UICalendar.COMPONENT_TYPE, "org.richfaces.CalendarRenderer");
            ComponentHandler input = new InputDateTimeTagHandler(config);
            String styleClass = "inputDate";
            if (widget.getProperty("styleClass") != null) {
                styleClass += " " + (String) widget.getProperty("styleClass");
            }
            // always add a surrounding span tag with associated style class,
            // see NXP-14963
            input = helper.getHtmlComponentHandler(widgetTagConfigId,
                    FaceletHandlerHelper.getTagAttributes(helper.createAttribute("styleClass", styleClass)), input,
                    HtmlPanelGroup.COMPONENT_TYPE, null);
            String msgId = FaceletHandlerHelper.generateMessageId(ctx, widgetName);
            ComponentHandler message = helper.getMessageComponentHandler(widgetTagConfigId, msgId, widgetId, null);
            FaceletHandler[] handlers = { input, message };
            FaceletHandler h = new CompositeFaceletHandler(handlers);
            h.apply(ctx, parent);
        } else {
            ConverterConfig convertConfig = TagConfigFactory.createConverterConfig(tagConfig, widget.getTagConfigId(),
                    attributes, new LeafFaceletHandler(), javax.faces.convert.DateTimeConverter.CONVERTER_ID);
            ConverterHandler convert = new ConvertDateTimeHandler(convertConfig);
            FaceletHandler nextHandler = new CompositeFaceletHandler(new FaceletHandler[] { convert, leaf });
            ComponentHandler output = helper.getHtmlComponentHandler(widgetTagConfigId, attributes, nextHandler,
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

}
