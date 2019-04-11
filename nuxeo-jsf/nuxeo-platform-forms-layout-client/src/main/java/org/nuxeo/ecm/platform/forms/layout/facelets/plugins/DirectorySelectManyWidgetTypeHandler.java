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
 * $Id: DirectorySelectManyWidgetTypeHandler.java 30416 2008-02-21 19:10:37Z atchertchian $
 */

package org.nuxeo.ecm.platform.forms.layout.facelets.plugins;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.faces.component.UIComponent;
import javax.faces.component.html.HtmlColumn;
import javax.faces.component.html.HtmlDataTable;
import javax.faces.component.html.HtmlOutputText;
import javax.faces.component.html.HtmlSelectManyListbox;
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
import org.nuxeo.ecm.platform.ui.web.component.list.UIEditableList;
import org.nuxeo.ecm.platform.ui.web.component.seam.UIHtmlText;
import org.nuxeo.ecm.platform.ui.web.directory.DirectoryEntryOutputComponent;

import com.sun.faces.facelets.tag.TagAttributesImpl;

/**
 * Select many directory widget
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public class DirectorySelectManyWidgetTypeHandler extends AbstractDirectorySelectWidgetTypeHandler {

    public DirectorySelectManyWidgetTypeHandler(TagConfig config) {
        super(config);
    }

    protected String getEditComponentType() {
        return HtmlSelectManyListbox.COMPONENT_TYPE;
    }

    @Override
    public void apply(FaceletContext ctx, UIComponent parent, Widget widget) throws WidgetException, IOException {
        String mode = widget.getMode();
        if (BuiltinWidgetModes.EDIT.equals(mode)) {
            super.apply(ctx, parent, widget, getEditComponentType());
            return;
        }

        FaceletHandlerHelper helper = new FaceletHandlerHelper(tagConfig);
        FaceletHandler leaf = getNextHandler(ctx, tagConfig, widget, null, helper);
        String widgetName = widget.getName();
        String widgetTagConfigId = widget.getTagConfigId();

        // build value attribute for iteration component
        String valueAttributeName = "value";
        Map<String, Serializable> properties = widget.getProperties();
        TagAttribute valueAttr = null;
        if (properties.containsKey("value")) {
            valueAttr = helper.createAttribute(valueAttributeName, (String) properties.get("value"));
        }
        FieldDefinition[] fields = widget.getFieldDefinitions();
        if (fields != null && fields.length > 0) {
            FieldDefinition field = fields[0];
            valueAttr = helper.createAttribute(valueAttributeName,
                    ValueExpressionHelper.createExpressionString(widget.getValueName(), field));
        }
        if (valueAttr == null) {
            // don't bother
            return;
        }

        // build directory item attributes, using widget properties
        List<TagAttribute> attrs = new ArrayList<>();
        for (Map.Entry<String, Serializable> property : properties.entrySet()) {
            if (!"value".equals(property.getKey())) {
                Serializable value = property.getValue();
                TagAttribute attr = null;
                if (value instanceof String) {
                    attr = helper.createAttribute(property.getKey(), (String) value);
                } else if (value != null) {
                    attr = helper.createAttribute(property.getKey(), value.toString());
                }
                if (attr != null) {
                    attrs.add(attr);
                }
            }
        }
        if (BuiltinWidgetModes.isLikePlainMode(mode)) {
            attrs.add(helper.createAttribute("value", "#{model.rowData}"));
        } else {
            attrs.add(helper.createAttribute("value", "#{item}"));
        }
        TagAttributes dirEntryAttrs = FaceletHandlerHelper.getTagAttributes(attrs);
        ComponentHandler dirEntry = helper.getHtmlComponentHandler(widgetTagConfigId, dirEntryAttrs, leaf,
                DirectoryEntryOutputComponent.COMPONENT_TYPE, null);

        if (BuiltinWidgetModes.isLikePlainMode(mode)) {
            // use an iteration and a comma to separate items instead of an
            // HTML table component
            TagAttributes commaAttributes = FaceletHandlerHelper.getTagAttributes(
                    helper.createAttribute("value", ", "),
                    helper.createAttribute("rendered", "#{model.rowIndex < model.rowCount}"));
            ComponentHandler commaHandler = helper.getHtmlComponentHandler(widgetTagConfigId, commaAttributes, leaf,
                    HtmlOutputText.COMPONENT_TYPE, null);

            CompositeFaceletHandler childHandler = new CompositeFaceletHandler(new FaceletHandler[] { dirEntry,
                    commaHandler });

            TagAttributes itAttributes = FaceletHandlerHelper.getTagAttributes(valueAttr,
                    helper.createAttribute("model", "model"));
            ComponentHandler itHandler = helper.getHtmlComponentHandler(widgetTagConfigId, itAttributes, childHandler,
                    UIEditableList.COMPONENT_TYPE, null);
            itHandler.apply(ctx, parent);
        } else {
            // build a standard table
            ComponentHandler columnEntry = helper.getHtmlComponentHandler(widgetTagConfigId,
                    FaceletHandlerHelper.getTagAttributes(), dirEntry, HtmlColumn.COMPONENT_TYPE, null);

            TagAttributes iterationAttributes = FaceletHandlerHelper.getTagAttributes(
                    helper.createIdAttribute(ctx, widgetName), valueAttr, helper.createAttribute("var", "item"));

            ComponentHandler table = helper.getHtmlComponentHandler(widgetTagConfigId, iterationAttributes,
                    columnEntry, HtmlDataTable.COMPONENT_TYPE, null);

            if (BuiltinWidgetModes.PDF.equals(mode)) {
                // add a surrounding p:html tag handler
                FaceletHandler h = helper.getHtmlComponentHandler(widgetTagConfigId, new TagAttributesImpl(
                        new TagAttribute[0]), table, UIHtmlText.class.getName(), null);
                h.apply(ctx, parent);
            } else {
                table.apply(ctx, parent);
            }
        }
    }
}
