/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.forms.layout.facelets.plugins;

import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;

import javax.faces.component.UIComponent;
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.FaceletHandler;
import javax.faces.view.facelets.TagAttribute;
import javax.faces.view.facelets.TagAttributes;
import javax.faces.view.facelets.TagConfig;
import javax.faces.view.facelets.TagHandler;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.ecm.platform.forms.layout.api.BuiltinWidgetModes;
import org.nuxeo.ecm.platform.forms.layout.api.Widget;
import org.nuxeo.ecm.platform.forms.layout.api.exceptions.WidgetException;
import org.nuxeo.ecm.platform.forms.layout.facelets.FaceletHandlerHelper;
import org.nuxeo.ecm.platform.forms.layout.facelets.LayoutTagHandler;
import org.nuxeo.ecm.platform.ui.web.component.seam.UIHtmlText;
import org.nuxeo.ecm.platform.ui.web.tag.handler.TagConfigFactory;

import com.sun.faces.facelets.tag.TagAttributesImpl;

/**
 * Widget rendering a layout
 *
 * @author Anahide Tchertchian
 * @Since 5.4
 */
public class LayoutWidgetTypeHandler extends AbstractWidgetTypeHandler {

    public LayoutWidgetTypeHandler(TagConfig config) {
        super(config);
    }

    @Override
    public void apply(FaceletContext ctx, UIComponent parent, Widget widget) throws WidgetException, IOException {
        FaceletHandlerHelper helper = new FaceletHandlerHelper(tagConfig);
        String widgetId = widget.getId();
        String widgetMode = widget.getMode();

        TagAttributes attributes = helper.getTagAttributes(widget, Arrays.asList(new String[] { "mode" }), true, true);
        attributes = FaceletHandlerHelper.addTagAttribute(attributes, helper.createAttribute("id", widgetId));

        // add mode attribute
        String modeValue;
        Serializable modeFromProps = widget.getProperty("mode");
        if ((modeFromProps instanceof String) && !StringUtils.isBlank((String) modeFromProps)) {
            modeValue = (String) modeFromProps;
        } else {
            modeValue = widgetMode;
        }
        attributes = FaceletHandlerHelper.addTagAttribute(attributes, helper.createAttribute("mode", modeValue));

        FaceletHandler leaf = getNextHandler(ctx, tagConfig, widget, null, helper, false, false);
        String widgetTagConfigId = widget.getTagConfigId();
        TagConfig layoutTagConfig = TagConfigFactory.createTagConfig(tagConfig, widgetTagConfigId, attributes, leaf);
        TagHandler res = new LayoutTagHandler(layoutTagConfig);
        if (BuiltinWidgetModes.PDF.equals(widgetMode)) {
            // add a surrounding p:html tag handler
            FaceletHandler h = helper.getHtmlComponentHandler(widgetTagConfigId, new TagAttributesImpl(
                    new TagAttribute[0]), res, UIHtmlText.class.getName(), null);
            h.apply(ctx, parent);
        } else {
            res.apply(ctx, parent);
        }

    }

}
