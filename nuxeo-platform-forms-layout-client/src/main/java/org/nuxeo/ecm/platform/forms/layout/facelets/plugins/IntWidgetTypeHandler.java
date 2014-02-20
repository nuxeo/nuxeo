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
 * $Id: IntWidgetTypeHandler.java 30416 2008-02-21 19:10:37Z atchertchian $
 */

package org.nuxeo.ecm.platform.forms.layout.facelets.plugins;

import javax.faces.component.html.HtmlInputText;
import javax.faces.component.html.HtmlOutputText;
import javax.faces.convert.NumberConverter;
import javax.faces.view.facelets.ComponentHandler;
import javax.faces.view.facelets.CompositeFaceletHandler;
import javax.faces.view.facelets.ConverterConfig;
import javax.faces.view.facelets.ConverterHandler;
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.FaceletHandler;
import javax.faces.view.facelets.TagAttribute;
import javax.faces.view.facelets.TagAttributes;
import javax.faces.view.facelets.TagConfig;

import org.nuxeo.ecm.platform.forms.layout.api.BuiltinWidgetModes;
import org.nuxeo.ecm.platform.forms.layout.api.Widget;
import org.nuxeo.ecm.platform.forms.layout.api.exceptions.WidgetException;
import org.nuxeo.ecm.platform.forms.layout.facelets.FaceletHandlerHelper;
import org.nuxeo.ecm.platform.forms.layout.facelets.LeafFaceletHandler;
import org.nuxeo.ecm.platform.ui.web.component.seam.UIHtmlText;
import org.nuxeo.ecm.platform.ui.web.tag.handler.TagConfigFactory;

import com.sun.faces.facelets.tag.TagAttributesImpl;
import com.sun.faces.facelets.tag.jsf.core.ConvertNumberHandler;

/**
 * Int widget.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public class IntWidgetTypeHandler extends AbstractWidgetTypeHandler {

    private static final long serialVersionUID = 1495841177711755669L;

    @Override
    public FaceletHandler getFaceletHandler(FaceletContext ctx,
            TagConfig tagConfig, Widget widget, FaceletHandler[] subHandlers)
            throws WidgetException {
        FaceletHandlerHelper helper = new FaceletHandlerHelper(ctx, tagConfig);
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
        FaceletHandler leaf = null;
        if (subHandlers != null) {
            leaf = new CompositeFaceletHandler(subHandlers);
        } else {
            leaf = new LeafFaceletHandler();
        }
        if (BuiltinWidgetModes.EDIT.equals(mode)) {
            ConverterConfig convertConfig = TagConfigFactory.createConverterConfig(
                    tagConfig, widget.getTagConfigId(), new TagAttributesImpl(
                            new TagAttribute[0]), leaf,
                    NumberConverter.CONVERTER_ID);
            ConverterHandler convert = new ConvertNumberHandler(convertConfig);
            ComponentHandler input = helper.getHtmlComponentHandler(
                    widgetTagConfigId, attributes, convert,
                    HtmlInputText.COMPONENT_TYPE, null);
            String msgId = helper.generateMessageId(widgetName);
            ComponentHandler message = helper.getMessageComponentHandler(
                    widgetTagConfigId, msgId, widgetId, null);
            FaceletHandler[] handlers = { input, message };
            return new CompositeFaceletHandler(handlers);
        } else if (BuiltinWidgetModes.CSV.equals(mode)) {
            // default on text without any converter to ease format
            // configuration
            ComponentHandler output = helper.getHtmlComponentHandler(
                    widgetTagConfigId, attributes, leaf,
                    HtmlOutputText.COMPONENT_TYPE, null);
            return output;
        } else {
            // default on text with int converter for other modes
            ConverterConfig convertConfig = TagConfigFactory.createConverterConfig(
                    tagConfig, widget.getTagConfigId(), new TagAttributesImpl(
                            new TagAttribute[0]), leaf,
                    NumberConverter.CONVERTER_ID);
            ConverterHandler convert = new ConvertNumberHandler(convertConfig);
            ComponentHandler output = helper.getHtmlComponentHandler(
                    widgetTagConfigId, attributes, convert,
                    HtmlOutputText.COMPONENT_TYPE, null);
            if (BuiltinWidgetModes.PDF.equals(mode)) {
                // add a surrounding p:html tag handler
                return helper.getHtmlComponentHandler(widgetTagConfigId,
                        new TagAttributesImpl(new TagAttribute[0]), output,
                        UIHtmlText.class.getName(), null);
            } else {
                return output;
            }
        }
    }
}
