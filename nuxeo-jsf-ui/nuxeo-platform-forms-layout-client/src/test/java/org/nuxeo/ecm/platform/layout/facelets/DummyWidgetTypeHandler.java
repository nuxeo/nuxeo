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
 * $Id: TestWidgetTypeHandler.java 28498 2008-01-05 11:46:25Z sfermigier $
 */

package org.nuxeo.ecm.platform.layout.facelets;

import java.io.IOException;

import javax.faces.component.UIComponent;
import javax.faces.component.html.HtmlInputText;
import javax.faces.component.html.HtmlOutputText;
import javax.faces.view.facelets.ComponentHandler;
import javax.faces.view.facelets.CompositeFaceletHandler;
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.FaceletHandler;
import javax.faces.view.facelets.TagAttributes;
import javax.faces.view.facelets.TagConfig;

import org.nuxeo.ecm.platform.forms.layout.api.BuiltinWidgetModes;
import org.nuxeo.ecm.platform.forms.layout.api.Widget;
import org.nuxeo.ecm.platform.forms.layout.api.exceptions.WidgetException;
import org.nuxeo.ecm.platform.forms.layout.facelets.FaceletHandlerHelper;
import org.nuxeo.ecm.platform.forms.layout.facelets.plugins.AbstractWidgetTypeHandler;
import org.nuxeo.ecm.platform.ui.web.tag.handler.LeafFaceletHandler;

/**
 * Test widget that shows possibilities on how to write a widget tag handler.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public class DummyWidgetTypeHandler extends AbstractWidgetTypeHandler {

    public DummyWidgetTypeHandler(TagConfig config) {
        super(config);
    }

    @Override
    public void apply(FaceletContext ctx, UIComponent parent, Widget widget) throws WidgetException, IOException {
        FaceletHandlerHelper helper = new FaceletHandlerHelper(tagConfig);
        String mode = widget.getMode();
        FaceletHandler[] handlers = null;
        String originalId = helper.generateUniqueId(ctx);
        TagAttributes attributes = helper.getTagAttributes(originalId, widget);
        FaceletHandler leaf = new LeafFaceletHandler();
        String widgetTagConfigId = widget.getTagConfigId();
        if (BuiltinWidgetModes.VIEW.equals(mode)) {
            ComponentHandler output = helper.getHtmlComponentHandler(widgetTagConfigId, attributes, leaf,
                    HtmlOutputText.COMPONENT_TYPE, null);
            output.apply(ctx, parent);
        } else if (BuiltinWidgetModes.EDIT.equals(mode)) {
            ComponentHandler input = helper.getHtmlComponentHandler(widgetTagConfigId, attributes, leaf,
                    HtmlInputText.COMPONENT_TYPE, null);
            String msgId = FaceletHandlerHelper.generateUniqueId(ctx, originalId);
            ComponentHandler message = helper.getMessageComponentHandler(widgetTagConfigId, msgId, originalId,
                    "errorMessage");
            handlers = new FaceletHandler[] { input, message };
            FaceletHandler h = new CompositeFaceletHandler(handlers);
            h.apply(ctx, parent);
        }
    }

}
