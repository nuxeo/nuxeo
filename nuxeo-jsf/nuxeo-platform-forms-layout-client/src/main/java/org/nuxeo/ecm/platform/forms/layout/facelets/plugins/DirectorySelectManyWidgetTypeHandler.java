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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.forms.layout.api.BuiltinWidgetModes;
import org.nuxeo.ecm.platform.forms.layout.api.FieldDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.Widget;
import org.nuxeo.ecm.platform.forms.layout.api.exceptions.WidgetException;
import org.nuxeo.ecm.platform.forms.layout.facelets.FaceletHandlerHelper;
import org.nuxeo.ecm.platform.forms.layout.facelets.LeafFaceletHandler;
import org.nuxeo.ecm.platform.forms.layout.facelets.RenderVariables;
import org.nuxeo.ecm.platform.forms.layout.facelets.ValueExpressionHelper;
import org.nuxeo.ecm.platform.ui.web.component.seam.UIHtmlText;
import org.nuxeo.ecm.platform.ui.web.directory.DirectoryEntryOutputComponent;
import org.nuxeo.ecm.platform.ui.web.directory.SelectManyListboxComponent;

import com.sun.facelets.FaceletContext;
import com.sun.facelets.FaceletHandler;
import com.sun.facelets.tag.CompositeFaceletHandler;
import com.sun.facelets.tag.TagAttribute;
import com.sun.facelets.tag.TagAttributes;
import com.sun.facelets.tag.TagConfig;
import com.sun.facelets.tag.jsf.ComponentHandler;

/**
 * Select many directory widget
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public class DirectorySelectManyWidgetTypeHandler extends
        AbstractWidgetTypeHandler {

    private static final long serialVersionUID = 1495841177711755669L;

    @SuppressWarnings("unused")
    private static final Log log = LogFactory.getLog(DirectorySelectManyWidgetTypeHandler.class);

    @Override
    public FaceletHandler getFaceletHandler(FaceletContext ctx,
            TagConfig tagConfig, Widget widget, FaceletHandler[] subHandlers)
            throws WidgetException {
        FaceletHandler leaf = new LeafFaceletHandler();
        FaceletHandlerHelper helper = new FaceletHandlerHelper(ctx, tagConfig);
        String mode = widget.getMode();
        String widgetId = widget.getId();
        String widgetName = widget.getName();
        TagAttributes attributes;
        if (BuiltinWidgetModes.isLikePlainMode(mode)) {
            // use attributes without id
            attributes = helper.getTagAttributes(widget);
        } else {
            attributes = helper.getTagAttributes(widgetId, widget);
        }
        if (BuiltinWidgetModes.EDIT.equals(mode)) {
            ComponentHandler input = helper.getHtmlComponentHandler(attributes,
                    leaf, SelectManyListboxComponent.COMPONENT_TYPE, null);
            String msgId = helper.generateMessageId(widgetName);
            ComponentHandler message = helper.getMessageComponentHandler(msgId,
                    widgetId, null);
            FaceletHandler[] handlers = { input, message };
            return new CompositeFaceletHandler(handlers);
        } else {
            Map<String, Serializable> properties = widget.getProperties();
            // get value attribute
            TagAttribute valueAttr = null;
            if (properties.containsKey("value")) {
                valueAttr = helper.createAttribute("value",
                        (String) properties.get("value"));
            }
            FieldDefinition[] fields = widget.getFieldDefinitions();
            if (fields != null && fields.length > 0) {
                FieldDefinition field = fields[0];
                valueAttr = helper.createAttribute(
                        RenderVariables.globalVariables.value.name(),
                        ValueExpressionHelper.createExpressionString(
                                widget.getValueName(), field));
            }
            if (valueAttr == null) {
                // don't bother
                return leaf;
            }

            TagAttributes tableAttributes = FaceletHandlerHelper.getTagAttributes(
                    helper.createIdAttribute(widgetName), valueAttr,
                    helper.createAttribute("var", "item"));
            List<TagAttribute> attrs = new ArrayList<TagAttribute>();
            // first fill with widget properties
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
            attrs.add(helper.createAttribute("value", "#{item}"));
            TagAttributes dirEntryAttrs = FaceletHandlerHelper.getTagAttributes(attrs);
            // XXX facelets do not like null attributes
            TagAttributes columnAttrs = new TagAttributes(new TagAttribute[0]);

            ComponentHandler dirEntry = helper.getHtmlComponentHandler(
                    dirEntryAttrs, leaf,
                    DirectoryEntryOutputComponent.COMPONENT_TYPE, null);
            ComponentHandler columnEntry = helper.getHtmlComponentHandler(
                    columnAttrs, dirEntry, HtmlColumn.COMPONENT_TYPE, null);
            ComponentHandler table = helper.getHtmlComponentHandler(
                    tableAttributes, columnEntry, HtmlDataTable.COMPONENT_TYPE,
                    null);

            if (BuiltinWidgetModes.PDF.equals(mode)) {
                // add a surrounding p:html tag handler
                return helper.getHtmlComponentHandler(new TagAttributes(
                        new TagAttribute[0]), table,
                        UIHtmlText.class.getName(), null);
            } else {
                return table;
            }
        }
    }
}
