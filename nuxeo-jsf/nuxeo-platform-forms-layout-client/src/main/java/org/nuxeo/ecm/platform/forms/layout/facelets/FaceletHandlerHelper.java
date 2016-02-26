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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.el.ExpressionFactory;
import javax.el.ValueExpression;
import javax.faces.component.html.HtmlMessage;
import javax.faces.component.html.HtmlOutputText;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.validator.Validator;
import javax.faces.view.facelets.ComponentConfig;
import javax.faces.view.facelets.ComponentHandler;
import javax.faces.view.facelets.ConverterConfig;
import javax.faces.view.facelets.ConverterHandler;
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.FaceletHandler;
import javax.faces.view.facelets.TagAttribute;
import javax.faces.view.facelets.TagAttributes;
import javax.faces.view.facelets.TagConfig;
import javax.faces.view.facelets.TagHandler;
import javax.faces.view.facelets.ValidatorConfig;
import javax.faces.view.facelets.ValidatorHandler;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.forms.layout.actions.NuxeoLayoutManagerBean;
import org.nuxeo.ecm.platform.forms.layout.api.FieldDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.Widget;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetSelectOption;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetSelectOptions;
import org.nuxeo.ecm.platform.forms.layout.service.WebLayoutManager;
import org.nuxeo.ecm.platform.ui.web.binding.alias.AliasTagHandler;
import org.nuxeo.ecm.platform.ui.web.tag.fn.Functions;
import org.nuxeo.ecm.platform.ui.web.tag.handler.GenericHtmlComponentHandler;
import org.nuxeo.ecm.platform.ui.web.tag.handler.SetTagHandler;
import org.nuxeo.ecm.platform.ui.web.tag.handler.TagConfigFactory;
import org.nuxeo.ecm.platform.ui.web.util.ComponentTagUtils;
import org.nuxeo.runtime.api.Framework;

import com.sun.faces.facelets.tag.TagAttributeImpl;
import com.sun.faces.facelets.tag.TagAttributesImpl;
import com.sun.faces.facelets.tag.ui.ComponentRef;
import com.sun.faces.facelets.tag.ui.ComponentRefHandler;

/**
 * Helpers for layout/widget handlers.
 * <p>
 * Helps generating custom tag handlers and custom tag attributes.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public final class FaceletHandlerHelper {

    private static final Log log = LogFactory.getLog(FaceletHandlerHelper.class);

    public static final String LAYOUT_ID_PREFIX = "nxl_";

    public static final String WIDGET_ID_PREFIX = "nxw_";

    public static final String MESSAGE_ID_SUFFIX = "_message";

    /**
     * @since 6.0
     */
    public static final String DEV_CONTAINER_ID_SUFFIX = "_dev_container";

    /**
     * @since 6.0
     */
    public static final String DEV_REGION_ID_SUFFIX = "_dev_region";

    /**
     * @since 6.0
     */
    public static String DEV_MODE_DISABLED_VARIABLE = "nuxeoLayoutDevModeDisabled";

    private static final Pattern UNIQUE_ID_STRIP_PATTERN = Pattern.compile("(.*)(_[0-9]+)");

    /**
     * @since 5.7
     */
    public static final String DIR_PROPERTY = "dir";

    /**
     * @since 5.7
     */
    public static final String DIR_AUTO = "auto";

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
        FacesContext faces = context.getFacesContext();
        NuxeoLayoutIdManagerBean bean = lookupIdBean(faces);
        return bean.generateUniqueId(base);
    }

    protected static NuxeoLayoutIdManagerBean lookupIdBean(FacesContext ctx) {
        String expr = "#{" + NuxeoLayoutIdManagerBean.NAME + "}";
        NuxeoLayoutIdManagerBean bean = (NuxeoLayoutIdManagerBean) ctx.getApplication().evaluateExpressionGet(ctx,
                expr, Object.class);
        if (bean == null) {
            throw new RuntimeException("Managed bean not found: " + expr);
        }
        return bean;
    }

    /**
     * Strips given base of any ending counter that would conflict with potential already generated unique ids
     *
     * @since 5.7
     */
    protected static String stripUniqueIdBase(String base) {
        if (base != null) {
            Matcher m = UNIQUE_ID_STRIP_PATTERN.matcher(base);
            if (m.matches()) {
                base = m.group(1);
                return stripUniqueIdBase(base);
            }
        }
        return base;
    }

    /**
     * Generates a unique id from counters persisted in given map
     *
     * @since 5.7
     * @deprecated since 7.2, see {@link NuxeoLayoutIdManagerBean}
     */
    @Deprecated
    public static String generateUniqueId(String base, Map<String, Integer> counters) {
        // strip base of any remnant counter name
        base = stripUniqueIdBase(base);
        // increment in map
        Integer cnt = counters.get(base);
        if (cnt == null) {
            counters.put(base, new Integer(0));
            return base;
        } else {
            int i = cnt.intValue() + 1;
            counters.put(base, new Integer(i));
            return base + "_" + i;
        }
    }

    /**
     * @throws IllegalArgumentException if the given string is null or empty.
     */
    protected static String generateValidIdString(String base) {
        if (base == null) {
            throw new IllegalArgumentException(base);
        }
        int n = base.length();
        if (n < 1) {
            throw new IllegalArgumentException(base);
        }
        return Functions.jsfTagIdEscape(base);
    }

    public String generateWidgetId(String widgetName) {
        return generateUniqueId(WIDGET_ID_PREFIX + widgetName);
    }

    public String generateLayoutId(String layoutName) {
        return generateUniqueId(LAYOUT_ID_PREFIX + layoutName);
    }

    public String generateMessageId(String widgetName) {
        return generateUniqueId(WIDGET_ID_PREFIX + widgetName + MESSAGE_ID_SUFFIX);
    }

    /**
     * @since 6.0
     */
    public String generateDevRegionId(String widgetName) {
        return generateUniqueId(WIDGET_ID_PREFIX + widgetName + DEV_REGION_ID_SUFFIX);
    }

    /**
     * @since 6.0
     */
    public String generateDevContainerId(String widgetName) {
        return generateUniqueId(WIDGET_ID_PREFIX + widgetName + DEV_CONTAINER_ID_SUFFIX);
    }

    /**
     * Creates a unique id and returns corresponding attribute, using given string id as base.
     */
    public TagAttribute createIdAttribute(String base) {
        String value = generateUniqueId(base);
        return new TagAttributeImpl(tagConfig.getTag().getLocation(), "", "id", "id", value);
    }

    /**
     * Creates an attribute with given name and value.
     * <p>
     * The attribute namespace is assumed to be empty.
     */
    public TagAttribute createAttribute(String name, String value) {
        if (value == null || value instanceof String) {
            return new TagAttributeImpl(tagConfig.getTag().getLocation(), "", name, name, value);
        }
        return null;
    }

    /**
     * Returns true if a reference tag attribute should be created for given property value.
     * <p>
     * Reference tag attributes are using a non-literal EL expression so that this property value is not kept (cached)
     * in the component on ajax refresh.
     * <p>
     * Of course property values already representing an expression cannot be mapped as is because they would need to be
     * resolved twice.
     * <p>
     * Converters and validators cannot be referenced either because components expect corresponding value expressions
     * to resolve to a {@link Converter} or {@link Validator} instance (instead of the converter of validator id).
     */
    public boolean shouldCreateReferenceAttribute(String key, Serializable value) {
        // FIXME: NXP-7004: make this configurable per widget type and mode or
        // JSF component
        if ((value instanceof String)
                && (ComponentTagUtils.isValueReference((String) value) || "converter".equals(key)
                        || "validator".equals(key)
                        // size is mistaken for the properties map size because
                        // of jboss el resolvers
                        || "size".equals(key)
                        // richfaces calendar does not resolve EL expressions
                        // correctly
                        || "showApplyButton".equals(key) || "defaultTime".equals(key))) {
            return false;
        }
        return true;
    }

    public static TagAttributes getTagAttributes(TagAttribute... attributes) {
        if (attributes == null || attributes.length == 0) {
            return new TagAttributesImpl(new TagAttribute[0]);
        }
        return new TagAttributesImpl(attributes);
    }

    public static TagAttributes getTagAttributes(List<TagAttribute> attributes) {
        return getTagAttributes(attributes.toArray(new TagAttribute[0]));
    }

    public static TagAttributes addTagAttribute(TagAttributes orig, TagAttribute newAttr) {
        if (orig == null) {
            return new TagAttributesImpl(new TagAttribute[] { newAttr });
        }
        List<TagAttribute> allAttrs = new ArrayList<TagAttribute>(Arrays.asList(orig.getAll()));
        allAttrs.add(newAttr);
        return getTagAttributes(allAttrs);
    }

    /**
     * Copies tag attributes with given names from the tag config, using given id as base for the id attribute.
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
        return new TagAttributesImpl(attrs);
    }

    /**
     * Creates tag attributes using given widget properties and field definitions.
     * <p>
     * Assumes the "value" attribute has to be computed from the first field definition, using the "value" expression
     * (see widget type tag handler exposed values).
     */
    public TagAttributes getTagAttributes(String id, Widget widget) {
        // add id and value computed from fields
        TagAttributes widgetAttrs = getTagAttributes(widget);
        return addTagAttribute(widgetAttrs, createAttribute("id", id));
    }

    public TagAttributes getTagAttributes(Widget widget) {
        return getTagAttributes(widget, null, true);
    }

    /**
     * @since 5.5
     */
    public TagAttributes getTagAttributes(Widget widget, List<String> excludedProperties,
            boolean bindFirstFieldDefinition) {
        return getTagAttributes(widget, excludedProperties, bindFirstFieldDefinition, false);
    }

    /**
     * Return tag attributes for this widget, including value mapping from field definitions and properties
     *
     * @since 5.6
     * @param widget the widget to generate tag attributes for
     * @param excludedProperties the properties to exclude from tag attributes
     * @param bindFirstFieldDefinition if true, the first field definition will be bound to the tag attribute named
     *            "value"
     * @param defaultToValue if true, and there are no field definitions, tag attribute named "value" will be mapped to
     *            the current widget value name (e.g the layout value in most cases, or the parent widget value if
     *            widget is a sub widget)
     */
    public TagAttributes getTagAttributes(Widget widget, List<String> excludedProperties,
            boolean bindFirstFieldDefinition, boolean defaultToValue) {
        List<TagAttribute> attrs = new ArrayList<TagAttribute>();
        if (bindFirstFieldDefinition) {
            FieldDefinition field = null;
            FieldDefinition[] fields = widget.getFieldDefinitions();
            if (fields != null && fields.length > 0) {
                field = fields[0];
            }
            if (field != null || defaultToValue) {
                // bind value to first field definition or current value name
                TagAttribute valueAttr = createAttribute("value",
                        ValueExpressionHelper.createExpressionString(widget.getValueName(), field));
                attrs.add(valueAttr);
            }
        }
        // fill with widget properties
        List<TagAttribute> propertyAttrs = getTagAttributes(widget.getProperties(), excludedProperties, true,
                widget.getType(), widget.getTypeCategory(), widget.getMode());
        if (propertyAttrs != null) {
            attrs.addAll(propertyAttrs);
        }
        return getTagAttributes(attrs);
    }

    /**
     * @since 5.5, signature changed on 5.6 to include parameters widgetType and widgetMode.
     */
    public List<TagAttribute> getTagAttributes(Map<String, Serializable> properties, List<String> excludedProperties,
            boolean useReferenceProperties, String widgetType, String widgetTypeCategory, String widgetMode) {
        WebLayoutManager service = Framework.getService(WebLayoutManager.class);
        List<TagAttribute> attrs = new ArrayList<TagAttribute>();
        if (properties != null) {
            for (Map.Entry<String, Serializable> prop : properties.entrySet()) {
                TagAttribute attr;
                String key = prop.getKey();
                if (excludedProperties != null && excludedProperties.contains(key)) {
                    continue;
                }
                Serializable valueInstance = prop.getValue();
                if (!useReferenceProperties
                        || !service.referencePropertyAsExpression(key, valueInstance, widgetType, widgetTypeCategory,
                                widgetMode, null)) {
                    if (valueInstance == null || valueInstance instanceof String) {
                        // FIXME: this will not be updated correctly using ajax
                        attr = createAttribute(key, (String) valueInstance);
                    } else {
                        attr = createAttribute(key, valueInstance.toString());
                    }
                } else {
                    // create a reference so that it's a real expression
                    // and it's not kept (cached) in a component value on
                    // ajax refresh
                    attr = createAttribute(key,
                            "#{" + RenderVariables.widgetVariables.widget.name() + ".properties." + key + "}");
                }
                attrs.add(attr);
            }
        }
        return attrs;
    }

    /**
     * @since 6.0
     */
    public TagAttributes getTagAttributes(WidgetSelectOption selectOption, Map<String, Serializable> additionalProps) {
        Map<String, Serializable> props = getSelectOptionProperties(selectOption);
        if (additionalProps != null) {
            props.putAll(additionalProps);
        }
        List<TagAttribute> attrs = getTagAttributes(props, null, false, null, null, null);
        if (attrs == null) {
            attrs = Collections.emptyList();
        }
        return getTagAttributes(attrs);
    }

    public TagAttributes getTagAttributes(WidgetSelectOption selectOption) {
        return getTagAttributes(selectOption, null);
    }

    public Map<String, Serializable> getSelectOptionProperties(WidgetSelectOption selectOption) {
        Map<String, Serializable> map = new HashMap<String, Serializable>();
        if (selectOption != null) {
            Serializable value = selectOption.getValue();
            if (value != null) {
                map.put("value", value);
            }
            String var = selectOption.getVar();
            if (var != null) {
                map.put("var", var);
            }
            String itemLabel = selectOption.getItemLabel();
            if (itemLabel != null) {
                map.put("itemLabel", itemLabel);
            }
            String itemValue = selectOption.getItemValue();
            if (itemValue != null) {
                map.put("itemValue", itemValue);
            }
            Serializable itemDisabled = selectOption.getItemDisabled();
            if (itemDisabled != null) {
                map.put("itemDisabled", itemDisabled);
            }
            Serializable itemRendered = selectOption.getItemRendered();
            if (itemRendered != null) {
                map.put("itemRendered", itemRendered);
            }
            if (selectOption instanceof WidgetSelectOptions) {
                WidgetSelectOptions selectOptions = (WidgetSelectOptions) selectOption;
                Boolean caseSensitive = selectOptions.getCaseSensitive();
                if (caseSensitive != null) {
                    map.put("caseSensitive", caseSensitive);
                }
                String ordering = selectOptions.getOrdering();
                if (ordering != null) {
                    map.put("ordering", ordering);
                }
            }
        }
        return map;
    }

    /**
     * @deprecated since 5.4.2, use
     *             {@link FaceletHandlerHelper#getHtmlComponentHandler(String, TagAttributes, FaceletHandler, String, String)}
     *             instead.
     */
    @Deprecated
    public ComponentHandler getHtmlComponentHandler(TagAttributes attributes, FaceletHandler nextHandler,
            String componentType, String rendererType) {
        return getHtmlComponentHandler(null, attributes, nextHandler, componentType, rendererType);
    }

    /**
     * Returns an html component handler for this configuration.
     * <p>
     * Next handler cannot be null, use {@link org.nuxeo.ecm.platform.ui.web.tag.handler.LeafFaceletHandler} if no next
     * handler is needed.
     */
    public ComponentHandler getHtmlComponentHandler(String tagConfigId, TagAttributes attributes,
            FaceletHandler nextHandler, String componentType, String rendererType) {
        ComponentConfig config = TagConfigFactory.createComponentConfig(tagConfig, tagConfigId, attributes,
                nextHandler, componentType, rendererType);
        return new GenericHtmlComponentHandler(config);
    }

    /**
     * @deprecated since 5.4.2, use {@link FaceletHandlerHelper#getErrorComponentHandler(String, String)} instead.
     */
    @Deprecated
    public ComponentHandler getErrorComponentHandler(String errorMessage) {
        return getErrorComponentHandler(null, errorMessage);
    }

    /**
     * Component handler that displays an error on interface
     */
    public ComponentHandler getErrorComponentHandler(String tagConfigId, String errorMessage) {
        FaceletHandler leaf = new org.nuxeo.ecm.platform.ui.web.tag.handler.LeafFaceletHandler();
        TagAttribute valueAttr = createAttribute("value",
                "<span style=\"color:red;font-weight:bold;\">ERROR: " + errorMessage + "</span><br />");
        TagAttribute escapeAttr = createAttribute("escape", "false");
        ComponentHandler output = getHtmlComponentHandler(tagConfigId,
                FaceletHandlerHelper.getTagAttributes(valueAttr, escapeAttr), leaf, HtmlOutputText.COMPONENT_TYPE, null);
        return output;
    }

    /**
     * @deprecated since 5.4.2, use
     *             {@link FaceletHandlerHelper#getConvertHandler(String, TagAttributes, FaceletHandler, String)}
     *             instead.
     */
    @Deprecated
    public ConverterHandler getConvertHandler(TagAttributes attributes, FaceletHandler nextHandler, String converterId) {
        return getConvertHandler(null, attributes, nextHandler, converterId);
    }

    /**
     * Returns a convert handler for this configuration.
     * <p>
     * Next handler cannot be null, use {@link org.nuxeo.ecm.platform.ui.web.tag.handler.LeafFaceletHandler} if no next
     * handler is needed.
     */
    public ConverterHandler getConvertHandler(String tagConfigId, TagAttributes attributes, FaceletHandler nextHandler,
            String converterId) {
        ConverterConfig config = TagConfigFactory.createConverterConfig(tagConfig, tagConfigId, attributes,
                nextHandler, converterId);
        return new ConverterHandler(config);
    }

    /**
     * @deprecated since 5.4.2, use
     *             {@link FaceletHandlerHelper#getValidateHandler(String, TagAttributes, FaceletHandler, String)}
     *             instead.
     */
    @Deprecated
    public ValidatorHandler getValidateHandler(TagAttributes attributes, FaceletHandler nextHandler, String validatorId) {
        return getValidateHandler(null, attributes, nextHandler, validatorId);
    }

    /**
     * Returns a validate handler for this configuration.
     * <p>
     * Next handler cannot be null, use {@link org.nuxeo.ecm.platform.ui.web.tag.handler.LeafFaceletHandler} if no next
     * handler is needed.
     */
    public ValidatorHandler getValidateHandler(String tagConfigId, TagAttributes attributes,
            FaceletHandler nextHandler, String validatorId) {
        ValidatorConfig config = TagConfigFactory.createValidatorConfig(tagConfig, tagConfigId, attributes,
                nextHandler, validatorId);
        return new ValidatorHandler(config);
    }

    /**
     * @deprecated since 5.4.2, use
     *             {@link FaceletHandlerHelper#getMessageComponentHandler(String, String, String, String)} instead.
     */
    @Deprecated
    public ComponentHandler getMessageComponentHandler(String id, String forId, String styleClass) {
        return getMessageComponentHandler(null, id, forId, styleClass);
    }

    /**
     * Returns a message component handler with given attributes.
     * <p>
     * Uses component type "javax.faces.HtmlMessage" and renderer type "javax.faces.Message".
     */
    public ComponentHandler getMessageComponentHandler(String tagConfigId, String id, String forId, String styleClass) {
        TagAttribute forAttr = createAttribute("for", forId);
        TagAttribute idAttr = createAttribute("id", id);
        if (styleClass == null) {
            // default style class
            styleClass = "errorMessage";
        }
        TagAttribute styleAttr = createAttribute("styleClass", styleClass);
        TagAttributes attributes = getTagAttributes(forAttr, idAttr, styleAttr);
        ComponentConfig config = TagConfigFactory.createComponentConfig(tagConfig, tagConfigId, attributes,
                new org.nuxeo.ecm.platform.ui.web.tag.handler.LeafFaceletHandler(), HtmlMessage.COMPONENT_TYPE, null);
        return new ComponentHandler(config);
    }

    /**
     * @since 5.6
     */
    public FaceletHandler getAliasTagHandler(String tagConfigId, Map<String, ValueExpression> variables,
            List<String> blockedPatterns, FaceletHandler nextHandler) {
        FaceletHandler currentHandler = nextHandler;
        if (variables != null) {
            currentHandler = getBareAliasTagHandler(tagConfigId, variables, blockedPatterns, nextHandler);
        }
        return currentHandler;
    }

    protected TagHandler getBareAliasTagHandler(String tagConfigId, Map<String, ValueExpression> variables,
            List<String> blockedPatterns, FaceletHandler nextHandler) {
        // XXX also set id? cache? anchor?
        ComponentConfig config = TagConfigFactory.createAliasTagConfig(tagConfig, tagConfigId, getTagAttributes(),
                nextHandler);
        AliasTagHandler alias = new AliasTagHandler(config, variables, blockedPatterns);
        // NXP-18639: always wrap next alias handler in a component ref for tagConfigId to be taken into account and
        // anchored in the view with this id.
        ComponentConfig ref = TagConfigFactory.createComponentConfig(tagConfig, tagConfigId, getTagAttributes(), alias,
                ComponentRef.COMPONENT_TYPE, null);
        return new ComponentRefHandler(ref);
    }

    /**
     * @since 6.0
     */
    public static boolean isDevModeEnabled(FaceletContext ctx) {
        // avoid stack overflow when using layout tags within the dev
        // handler
        if (Framework.isDevModeSet()) {
            NuxeoLayoutManagerBean bean = lookupBean(ctx.getFacesContext());
            if (bean.isDevModeSet()) {
                ExpressionFactory eFactory = ctx.getExpressionFactory();
                ValueExpression disableDevAttr = eFactory.createValueExpression(ctx,
                        "#{" + DEV_MODE_DISABLED_VARIABLE + "}", Boolean.class);
                if (!Boolean.TRUE.equals(disableDevAttr.getValue(ctx))) {
                    return true;
                }
            }
        }
        return false;
    }

    protected static NuxeoLayoutManagerBean lookupBean(FacesContext ctx) {
        String expr = "#{" + NuxeoLayoutManagerBean.NAME + "}";
        NuxeoLayoutManagerBean bean = (NuxeoLayoutManagerBean) ctx.getApplication().evaluateExpressionGet(ctx, expr,
                Object.class);
        if (bean == null) {
            log.error("Managed bean not found: " + expr);
            return null;
        }
        return bean;
    }

    /**
     * @since 6.0
     */
    public FaceletHandler getDisableDevModeTagHandler(String tagConfigId, FaceletHandler nextHandler) {
        ComponentConfig config = TagConfigFactory.createAliasTagConfig(tagConfig, tagConfigId,
                DEV_MODE_DISABLED_VARIABLE, "true", "true", "false", nextHandler);
        return new SetTagHandler(config);
    }
}
