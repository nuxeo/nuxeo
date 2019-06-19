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
 * $Id: FileWidgetTypeHandler.java 30416 2008-02-21 19:10:37Z atchertchian $
 */

package org.nuxeo.ecm.platform.forms.layout.facelets.plugins;

import java.io.IOException;

import javax.faces.component.UIComponent;
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
import org.nuxeo.ecm.platform.ui.web.component.file.UIInputFile;
import org.nuxeo.ecm.platform.ui.web.component.file.UIOutputFile;
import org.nuxeo.ecm.platform.ui.web.component.seam.UIHtmlText;

import com.sun.faces.facelets.tag.TagAttributesImpl;

/**
 * File widget.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public class FileWidgetTypeHandler extends AbstractWidgetTypeHandler {

    public FileWidgetTypeHandler(TagConfig config) {
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
        // add filename from field definition
        FieldDefinition[] fields = widget.getFieldDefinitions();
        if (fields != null && fields.length > 1) {
            FieldDefinition filenameField = fields[1];
            TagAttribute filenameAttr = helper.createAttribute("filename",
                    ValueExpressionHelper.createExpressionString(widget.getValueName(), filenameField));
            attributes = FaceletHandlerHelper.addTagAttribute(attributes, filenameAttr);
        }
        // file components do not support client behaviors => do not add input
        // slot
        boolean isEdit = BuiltinWidgetModes.EDIT.equals(mode);
        FaceletHandler leaf = getNextHandler(ctx, tagConfig, widget, null, helper, false, isEdit);
        if (isEdit) {
            ComponentHandler input = helper.getHtmlComponentHandler(widgetTagConfigId, attributes, leaf,
                    UIInputFile.COMPONENT_TYPE, null);
            String msgId = FaceletHandlerHelper.generateMessageId(ctx, widgetName);
            ComponentHandler message = helper.getMessageComponentHandler(widgetTagConfigId, msgId, widgetId, null);
            FaceletHandler[] handlers = { input, message };
            FaceletHandler h = new CompositeFaceletHandler(handlers);
            h.apply(ctx, parent);
        } else {
            // TODO: handle PLAIN and PDF mode better?
            ComponentHandler output = helper.getHtmlComponentHandler(widgetTagConfigId, attributes, leaf,
                    UIOutputFile.COMPONENT_TYPE, null);
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
