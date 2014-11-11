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
 * $Id: DirectorySelectManyWidgetTypeHandler.java 30416 2008-02-21 19:10:37Z atchertchian $
 */

package org.nuxeo.ecm.platform.forms.layout.facelets.plugins;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
public class DirectorySelectManyWidgetTypeHandler extends
        AbstractDirectorySelectWidgetTypeHandler {

    private static final long serialVersionUID = 1L;

    protected String getEditComponentType() {
        return HtmlSelectManyListbox.COMPONENT_TYPE;
    }

    @Override
    public FaceletHandler getFaceletHandler(FaceletContext ctx,
            TagConfig tagConfig, Widget widget, FaceletHandler[] subHandlers)
            throws WidgetException {
        String mode = widget.getMode();
        if (BuiltinWidgetModes.EDIT.equals(mode)) {
            return super.getFaceletHandler(ctx, tagConfig, widget, subHandlers,
                    getEditComponentType());
        }

        FaceletHandlerHelper helper = new FaceletHandlerHelper(ctx, tagConfig);
        FaceletHandler leaf = getNextHandler(ctx, tagConfig, widget,
                subHandlers, helper);
        String widgetName = widget.getName();
        String widgetTagConfigId = widget.getTagConfigId();

        // build value attribute for iteration component
        String valueAttributeName = "value";
        Map<String, Serializable> properties = widget.getProperties();
        TagAttribute valueAttr = null;
        if (properties.containsKey("value")) {
            valueAttr = helper.createAttribute(valueAttributeName,
                    (String) properties.get("value"));
        }
        FieldDefinition[] fields = widget.getFieldDefinitions();
        if (fields != null && fields.length > 0) {
            FieldDefinition field = fields[0];
            valueAttr = helper.createAttribute(
                    valueAttributeName,
                    ValueExpressionHelper.createExpressionString(
                            widget.getValueName(), field));
        }
        if (valueAttr == null) {
            // don't bother
            return leaf;
        }

        // build directory item attributes, using widget properties
        List<TagAttribute> attrs = new ArrayList<TagAttribute>();
        for (Map.Entry<String, Serializable> property : properties.entrySet()) {
            if (!"value".equals(property.getKey())) {
                Serializable value = property.getValue();
                TagAttribute attr = null;
                if (value instanceof String) {
                    attr = helper.createAttribute(property.getKey(),
                            (String) value);
                } else if (value != null) {
                    attr = helper.createAttribute(property.getKey(),
                            value.toString());
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
        ComponentHandler dirEntry = helper.getHtmlComponentHandler(
                widgetTagConfigId, dirEntryAttrs, leaf,
                DirectoryEntryOutputComponent.COMPONENT_TYPE, null);

        if (BuiltinWidgetModes.isLikePlainMode(mode)) {
            // use an iteration and a comma to separate items instead of an
            // HTML table component
            TagAttributes commaAttributes = FaceletHandlerHelper.getTagAttributes(
                    helper.createAttribute("value", ", "),
                    helper.createAttribute("rendered",
                            "#{model.rowIndex < model.rowCount}"));
            ComponentHandler commaHandler = helper.getHtmlComponentHandler(
                    widgetTagConfigId, commaAttributes, leaf,
                    HtmlOutputText.COMPONENT_TYPE, null);

            CompositeFaceletHandler childHandler = new CompositeFaceletHandler(
                    new FaceletHandler[] { dirEntry, commaHandler });

            TagAttributes itAttributes = FaceletHandlerHelper.getTagAttributes(
                    valueAttr, helper.createAttribute("model", "model"));
            ComponentHandler itHandler = helper.getHtmlComponentHandler(
                    widgetTagConfigId, itAttributes, childHandler,
                    UIEditableList.COMPONENT_TYPE, null);

            return itHandler;
        } else {
            // build a standard table
            ComponentHandler columnEntry = helper.getHtmlComponentHandler(
                    widgetTagConfigId, FaceletHandlerHelper.getTagAttributes(),
                    dirEntry, HtmlColumn.COMPONENT_TYPE, null);

            TagAttributes iterationAttributes = FaceletHandlerHelper.getTagAttributes(
                    helper.createIdAttribute(widgetName), valueAttr,
                    helper.createAttribute("var", "item"));

            ComponentHandler table = helper.getHtmlComponentHandler(
                    widgetTagConfigId, iterationAttributes, columnEntry,
                    HtmlDataTable.COMPONENT_TYPE, null);

            if (BuiltinWidgetModes.PDF.equals(mode)) {
                // add a surrounding p:html tag handler
                return helper.getHtmlComponentHandler(widgetTagConfigId,
                        new TagAttributesImpl(new TagAttribute[0]), table,
                        UIHtmlText.class.getName(), null);
            } else {
                return table;
            }
        }
    }
}
