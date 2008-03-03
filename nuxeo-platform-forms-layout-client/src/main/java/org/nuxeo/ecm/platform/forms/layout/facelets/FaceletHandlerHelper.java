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
 * $Id: FaceletHandlerHelper.java 30553 2008-02-24 15:51:31Z atchertchian $
 */

package org.nuxeo.ecm.platform.forms.layout.facelets;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.faces.component.html.HtmlMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.forms.layout.api.FieldDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.Widget;

import com.sun.facelets.FaceletContext;
import com.sun.facelets.FaceletHandler;
import com.sun.facelets.tag.TagAttribute;
import com.sun.facelets.tag.TagAttributes;
import com.sun.facelets.tag.TagConfig;
import com.sun.facelets.tag.jsf.ComponentConfig;
import com.sun.facelets.tag.jsf.ComponentHandler;
import com.sun.facelets.tag.jsf.ConvertHandler;
import com.sun.facelets.tag.jsf.ConverterConfig;
import com.sun.facelets.tag.jsf.ValidateHandler;
import com.sun.facelets.tag.jsf.ValidatorConfig;
import com.sun.facelets.tag.jsf.html.HtmlComponentHandler;

/**
 * Helpers for layout/widget handlers.
 * <p>
 * Helps generating custom tag handlers and custom tag attributes.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 */
public final class FaceletHandlerHelper {

    @SuppressWarnings("unused")
    private static final Log log = LogFactory.getLog(FaceletHandlerHelper.class);

    public static final String LAYOUT_ID_PREFIX = "nxl_";

    public static final String WIDGET_ID_PREFIX = "nxw_";

    public static final String MESSAGE_ID_SUFFIX = "_message";

    final FaceletContext context;

    final TagConfig tagConfig;

    public FaceletHandlerHelper(FaceletContext context, TagConfig tagConfig) {
        this.context = context;
        this.tagConfig = tagConfig;
    }

    /**
     * Returns a id unique within the facelet context.
     */
    public String generateUniqueId() {
        String id;
        TagAttribute idAttr = tagConfig.getTag().getAttributes().get("id");
        if (idAttr != null) {
            id = idAttr.getValue(context);
        } else {
            id = context.getFacesContext().getViewRoot().createUniqueId();
        }
        return generateUniqueId(id);
    }

    /**
     * Returns a id unique within the facelet context using given id as base.
     */
    public String generateUniqueId(String base) {
        String id = context.generateUniqueId(base);
        return id;
    }

    public String generateWidgetId(String widgetName) {
        return generateUniqueId(WIDGET_ID_PREFIX + widgetName);
    }

    public String generateLayoutId(String layoutName) {
        return generateUniqueId(LAYOUT_ID_PREFIX + layoutName);
    }

    public String generateMessageId(String widgetName) {
        return generateUniqueId(WIDGET_ID_PREFIX + widgetName
                + MESSAGE_ID_SUFFIX);
    }

    /**
     * Creates a unique id and returns corresponding attribute, using given
     * string id as base.
     */
    public TagAttribute createIdAttribute(String base) {
        String value = generateUniqueId(base);
        return new TagAttribute(tagConfig.getTag().getLocation(), "", "id",
                "id", value);
    }

    /**
     * Creates an attribute with given name and value.
     * <p>
     * The attribute namespace is assumed to be empty.
     */
    public TagAttribute createAttribute(String name, Serializable value) {
        if (value == null || value instanceof String) {
            return new TagAttribute(tagConfig.getTag().getLocation(), "", name,
                    name, (String) value);
        }
        return null;
    }

    public static TagAttributes getTagAttributes(TagAttribute... attributes) {
        if (attributes == null || attributes.length == 0) {
            return null;
        }
        return new TagAttributes(attributes);
    }

    public static TagAttributes getTagAttributes(List<TagAttribute> attributes) {
        return getTagAttributes(attributes.toArray(new TagAttribute[] {}));
    }

    public static TagAttributes addTagAttribute(TagAttributes orig,
            TagAttribute newAttr) {
        if (orig == null) {
            return new TagAttributes(new TagAttribute[] { newAttr });
        }
        List<TagAttribute> allAttrs = new ArrayList<TagAttribute>(
                Arrays.asList(orig.getAll()));
        allAttrs.add(newAttr);
        return getTagAttributes(allAttrs);
    }

    /**
     * Copies tag attributes with given names from the tag config, using given
     * id as base for the id attribute.
     */
    public TagAttributes copyTagAttributes(String id, String... names) {
        List<TagAttribute> list = new ArrayList<TagAttribute>();
        list.add(createIdAttribute(id));
        for (String name : names) {
            if ("id".equals(name)) {
                // ignore
                continue;
            }
            TagAttribute attr = tagConfig.getTag().getAttributes().get(name);
            if (attr != null) {
                list.add(attr);
            }
        }
        TagAttribute[] attrs = list.toArray(new TagAttribute[list.size()]);
        return new TagAttributes(attrs);
    }

    /**
     * Creates tag attributes using given widget properties and field
     * definitions.
     * <p>
     * Assumes the "value" attribute has to be computed from the first field
     * definition, using the "value" expression (see widget type tag handler
     * exposed values).
     */
    public TagAttributes getTagAttributes(String id, Widget widget) {
        List<TagAttribute> attrs = new ArrayList<TagAttribute>();
        // add id and value computed from fields
        attrs.add(createAttribute("id", id));
        FieldDefinition[] fields = widget.getFieldDefinitions();
        if (fields != null && fields.length > 0) {
            FieldDefinition field = fields[0];
            TagAttribute valueAttr = createAttribute("value",
                    ValueExpressionHelper.createExpressionString(
                            widget.getValueName(), field));
            attrs.add(valueAttr);
        }
        // fill with widget properties
        Map<String, Serializable> properties = widget.getProperties();
        if (properties != null) {
            for (Map.Entry<String, Serializable> property : properties.entrySet()) {
                TagAttribute attr = createAttribute(property.getKey(),
                        property.getValue());
                if (attr != null) {
                    attrs.add(attr);
                }
            }
        }
        return getTagAttributes(attrs);
    }

    /**
     * Returns an html component handler for this configuration.
     * <p>
     * Next handler cannot be null, use {@link LeafFaceletHandler} if no next
     * handler is needed.
     */
    public ComponentHandler getHtmlComponentHandler(TagAttributes attributes,
            FaceletHandler nextHandler, String componentType,
            String rendererType) {
        ComponentConfig config = TagConfigFactory.createComponentConfig(
                tagConfig, attributes, nextHandler, componentType, rendererType);
        return new HtmlComponentHandler(config);
    }

    /**
     * Returns a convert handler for this configuration.
     * <p>
     * Next handler cannot be null, use {@link LeafFaceletHandler} if no next
     * handler is needed.
     */
    public ConvertHandler getConvertHandler(TagAttributes attributes,
            FaceletHandler nextHandler, String converterId) {
        ConverterConfig config = TagConfigFactory.createConverterConfig(
                tagConfig, attributes, nextHandler, converterId);
        return new ConvertHandler(config);
    }

    /**
     * Returns a validate handler for this configuration.
     * <p>
     * Next handler cannot be null, use {@link LeafFaceletHandler} if no next
     * handler is needed.
     */
    public ValidateHandler getValidateHandler(TagAttributes attributes,
            FaceletHandler nextHandler, String validatorId) {
        ValidatorConfig config = TagConfigFactory.createValidatorConfig(
                tagConfig, attributes, nextHandler, validatorId);
        return new ValidateHandler(config);
    }

    /**
     * Returns a message component handler with given attributes.
     * <p>
     * Uses component type "javax.faces.HtmlMessage" and renderer type
     * "javax.faces.Message".
     */
    public ComponentHandler getMessageComponentHandler(String id, String forId,
            String styleClass) {
        TagAttribute forAttr = createAttribute("for", forId);
        TagAttribute idAttr = createAttribute("id", id);
        if (styleClass == null) {
            // default style class
            styleClass = "errorMessage";
        }
        TagAttribute styleAttr = createAttribute("styleClass", styleClass);
        TagAttributes attributes = getTagAttributes(forAttr, idAttr, styleAttr);
        ComponentConfig config = TagConfigFactory.createComponentConfig(
                tagConfig, attributes, new LeafFaceletHandler(),
                HtmlMessage.COMPONENT_TYPE, null);
        return new ComponentHandler(config);
    }
}
