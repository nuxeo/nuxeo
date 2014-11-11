/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     <a href="mailto:tm@nuxeo.com">Thierry Martins</a>
 *
 * $Id: TextareaWidgetTypeHandler.java 30416 2008-02-21 19:10:37Z atchertchian $
 */

package org.nuxeo.ecm.platform.forms.layout.facelets.plugins;

import javax.faces.component.html.HtmlInputTextarea;
import javax.faces.component.html.HtmlOutputText;
import javax.faces.view.facelets.ComponentHandler;
import javax.faces.view.facelets.CompositeFaceletHandler;
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.FaceletHandler;
import javax.faces.view.facelets.TagAttribute;
import javax.faces.view.facelets.TagAttributes;
import javax.faces.view.facelets.TagConfig;

import org.nuxeo.ecm.platform.forms.layout.api.BuiltinWidgetModes;
import org.nuxeo.ecm.platform.forms.layout.api.Widget;
import org.nuxeo.ecm.platform.forms.layout.api.exceptions.WidgetException;
import org.nuxeo.ecm.platform.forms.layout.facelets.FaceletHandlerHelper;
import org.nuxeo.ecm.platform.ui.web.component.seam.UIHtmlText;

import com.sun.faces.facelets.tag.TagAttributesImpl;

/**
 * Textarea widget.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 * @author <a href="mailto:tm@nuxeo.com">Thierry Martins</a>
 */
public class TextareaWidgetTypeHandler extends AbstractWidgetTypeHandler {

    private static final long serialVersionUID = -771626672018944435L;

    // .wrapword{
    // white-space: -moz-pre-wrap !important; /* Mozilla, since 1999 */
    // white-space: -pre-wrap; /* Opera 4-6 */
    // white-space: -o-pre-wrap; /* Opera 7 */
    // white-space: pre-wrap; /* css-3 */
    // word-wrap: break-word; /* Internet Explorer 5.5+ */
    // }
    public static String WRAP_WORD_STYLE = "white-space: -moz-pre-wrap !important; white-space: -pre-wrap; white-space: -o-pre-wrap; white-space: pre-wrap; word-wrap: break-word;";

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
            // Make text fields automatically switch to right-to-left if
            // not told otherwise
            if (widget.getProperty(FaceletHandlerHelper.DIR_PROPERTY) == null) {
                TagAttribute dir = helper.createAttribute(
                        FaceletHandlerHelper.DIR_PROPERTY,
                        FaceletHandlerHelper.DIR_AUTO);
                attributes = FaceletHandlerHelper.addTagAttribute(attributes,
                        dir);
            }
        }
        FaceletHandler leaf = getNextHandler(ctx, tagConfig, widget,
                subHandlers, helper);
        if (BuiltinWidgetModes.EDIT.equals(mode)) {
            ComponentHandler input = helper.getHtmlComponentHandler(
                    widgetTagConfigId, attributes, leaf,
                    HtmlInputTextarea.COMPONENT_TYPE, null);
            String msgId = helper.generateMessageId(widgetName);
            ComponentHandler message = helper.getMessageComponentHandler(
                    widgetTagConfigId, msgId, widgetId, null);
            FaceletHandler[] handlers = { input, message };
            return new CompositeFaceletHandler(handlers);
        } else {
            // add styling for end of line characters to be displayed
            if (!BuiltinWidgetModes.EDIT.equals(mode)
                    && !BuiltinWidgetModes.isLikePlainMode(mode)
                    && widget.getProperty("style") == null) {
                TagAttribute escape = helper.createAttribute("style",
                        WRAP_WORD_STYLE);
                attributes = FaceletHandlerHelper.addTagAttribute(attributes,
                        escape);
            }
            ComponentHandler output = helper.getHtmlComponentHandler(
                    widgetTagConfigId, attributes, leaf,
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
