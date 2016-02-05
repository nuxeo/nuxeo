/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.platform.actions.facelets;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.el.ELException;
import javax.el.ExpressionFactory;
import javax.el.ValueExpression;
import javax.el.VariableMapper;
import javax.faces.FacesException;
import javax.faces.component.UIComponent;
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.FaceletHandler;
import javax.faces.view.facelets.MetaRuleset;
import javax.faces.view.facelets.MetaTagHandler;
import javax.faces.view.facelets.TagAttribute;
import javax.faces.view.facelets.TagAttributes;
import javax.faces.view.facelets.TagConfig;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.actions.Action;
import org.nuxeo.ecm.platform.forms.layout.api.BuiltinWidgetModes;
import org.nuxeo.ecm.platform.forms.layout.api.Widget;
import org.nuxeo.ecm.platform.forms.layout.api.impl.WidgetDefinitionImpl;
import org.nuxeo.ecm.platform.forms.layout.facelets.FaceletHandlerHelper;
import org.nuxeo.ecm.platform.forms.layout.facelets.RenderVariables;
import org.nuxeo.ecm.platform.forms.layout.facelets.WidgetTagHandler;
import org.nuxeo.ecm.platform.forms.layout.service.WebLayoutManager;
import org.nuxeo.ecm.platform.ui.web.binding.BlockingVariableMapper;
import org.nuxeo.ecm.platform.ui.web.tag.handler.FormTagHandler;
import org.nuxeo.ecm.platform.ui.web.tag.handler.TagConfigFactory;
import org.nuxeo.ecm.platform.ui.web.util.ComponentTagUtils;
import org.nuxeo.runtime.api.Framework;

import com.sun.faces.facelets.tag.TagAttributesImpl;

/**
 * Tag handler rendering an action given its type, applying corresponding widget tag handler, axposing additional
 * variables for action templates usage.
 *
 * @since 8.2
 */
public class ActionTagHandler extends MetaTagHandler {

    private static final Log log = LogFactory.getLog(ActionTagHandler.class);

    protected final TagConfig config;

    protected final TagAttribute action;

    protected final TagAttribute widgetName;

    protected final TagAttribute value;

    protected final TagAttribute mode;

    protected final TagAttribute addForm;

    protected final TagAttribute useAjaxForm;

    protected final TagAttribute formStyleClass;

    protected final TagAttribute postFilterMethod;

    protected final TagAttribute[] vars;

    protected final String[] reservedVarsArray = { "action", "widgetName", "value", "mode", "addForm", "useAjaxForm",
            "formStyleClass", "postFilterMethod" };

    public ActionTagHandler(TagConfig config) {
        super(config);
        this.config = config;

        action = getRequiredAttribute("action");
        widgetName = getAttribute("widgetName");
        value = getRequiredAttribute("value");
        mode = getAttribute("mode");
        addForm = getAttribute("addForm");
        useAjaxForm = getAttribute("useAjaxForm");
        formStyleClass = getAttribute("formStyleClass");
        postFilterMethod = getAttribute("postFilterMethod");

        vars = tag.getAttributes().getAll();
    }

    /**
     * Renders given widget resolving its {@link FaceletHandler} from {@link WebLayoutManager} configuration.
     * <p>
     * Variables exposed: {@link RenderVariables.globalVariables#value}, same variable suffixed with "_n" where n is the
     * widget level, and {@link RenderVariables.globalVariables#document}.
     */
    public void apply(FaceletContext ctx, UIComponent parent) throws IOException, FacesException, ELException {
        Action actionInstance = null;
        if (action != null) {
            actionInstance = (Action) action.getObject(ctx, Action.class);
        }
        if (actionInstance != null) {

            VariableMapper orig = ctx.getVariableMapper();
            try {
                BlockingVariableMapper vm = new BlockingVariableMapper(orig);
                ctx.setVariableMapper(vm);

                // build corresponding widget and adjust properties
                String wtype = actionInstance.getType();
                if (StringUtils.isBlank(wtype)) {
                    wtype = "link";
                }
                String wcat = "jsfAction";

                String modeValue = null;
                if (mode != null) {
                    modeValue = mode.getValue(ctx);
                }
                if (StringUtils.isBlank(modeValue)) {
                    modeValue = BuiltinWidgetModes.VIEW;
                }

                Map<String, Serializable> props = new HashMap<>();
                // put all action properties
                props.putAll(actionInstance.getProperties());
                if ("template".equals(wtype)) {
                    // avoid erasing template value from widget type configuration, and match template
                    String templateName = "template";
                    String modeTemplateName = "template" + "_" + modeValue;
                    if (BuiltinWidgetModes.VIEW.equals(modeValue) && props.containsKey(templateName)) {
                        props.put("action_template", props.get(templateName));
                    } else if (props.containsKey(modeTemplateName)) {
                        props.put("action_template", props.get(modeTemplateName));
                        props.remove(modeTemplateName);
                    }
                    props.remove(templateName);
                }
                // handle onclick
                StringBuilder fullOnclick = new StringBuilder();
                if (BuiltinWidgetModes.VIEW.equals(modeValue) && props.containsKey("confirmMessage")) {
                    String confirmMessage = (String) props.get("confirmMessage");
                    if (!StringUtils.isEmpty(confirmMessage)) {
                        fullOnclick.append(
                                "var message = \"#{nxu:translate(widgetProperty_confirmMessage, widgetProperty_confirmMessageArgs)}\";if (message != \"\" && !confirm(message)) {return false;};");
                    }
                }
                String confirm = actionInstance.getConfirm();
                if (!StringUtils.isEmpty(confirm)) {
                    fullOnclick.append(confirm).append(";");
                }
                String onclick = (String) actionInstance.getProperties().get("onclick");
                if (!StringUtils.isEmpty(onclick)) {
                    fullOnclick.append(onclick).append(";");
                }
                props.put("immediate", actionInstance.isImmediate());
                props.put("icon", actionInstance.getIcon());
                props.put("onclick", actionInstance.getConfirm());
                props.put("accessKey", actionInstance.getAccessKey());
                props.put("link", actionInstance.getLink());
                props.put("actionId", actionInstance.getId());
                props.put("action", actionInstance);
                if (useAjaxForm != null) {
                    props.put("useAjaxForm", useAjaxForm.getValue());
                }

                String valueName = value.getValue();
                String bareValueName = valueName;
                if (ComponentTagUtils.isStrictValueReference(valueName)) {
                    bareValueName = ComponentTagUtils.getBareValueName(valueName);
                }

                // add filtering method if needed
                if (!actionInstance.isFiltered()) {
                    // make sure variables are in the context for this filter resolution
                    ExpressionFactory eFactory = ctx.getExpressionFactory();
                    ValueExpression actionVe = eFactory.createValueExpression(actionInstance, Action.class);
                    vm.setVariable("action", actionVe);
                    vm.addBlockedPattern("action");

                    String bindingValue = bareValueName;
                    boolean bindingDone = false;
                    if (props.containsKey("actionContextDocument")) {
                        Object val = props.get("actionContextDocument");
                        if (val instanceof String && ComponentTagUtils.isStrictValueReference((String) val)) {
                            bindingValue = ComponentTagUtils.getBareValueName((String) val);
                            ValueExpression bindingVe = eFactory.createValueExpression(ctx, (String) val, Object.class);
                            vm.setVariable("actionContextDocument", bindingVe);
                            vm.addBlockedPattern("actionContextDocument");
                            bindingDone = true;
                        }
                    }
                    if (!bindingDone) {
                        // just bound current value to make expressions consistent
                        vm.setVariable("actionContextDocument", value.getValueExpression(ctx, DocumentModel.class));
                        vm.addBlockedPattern("actionContextDocument");
                    }

                    String method = null;
                    if (postFilterMethod != null) {
                        method = postFilterMethod.getValue(ctx);
                    }
                    if (StringUtils.isBlank(method)) {
                        method = "webActions.isAvailableForDocument";
                    }
                    String filterExpr = "#{" + method + "(" + bindingValue + ", action)}";
                    props.put("available", filterExpr);
                    props.put("enabled", filterExpr);
                } else {
                    props.put("available", actionInstance.getAvailable());
                    props.put("enabled", "true");
                }

                // add all extra props passed to the tag
                String widgetPropertyMarker = RenderVariables.widgetVariables.widgetProperty.name() + "_";
                List<String> reservedVars = Arrays.asList(reservedVarsArray);
                for (TagAttribute var : vars) {
                    String localName = var.getLocalName();
                    if (!reservedVars.contains(localName)) {
                        if (localName != null && localName.startsWith(widgetPropertyMarker)) {
                            localName = localName.substring(widgetPropertyMarker.length());
                        }
                        props.put(localName, var.getValue());
                    }
                }

                String widgetNameValue = null;
                if (widgetName != null) {
                    widgetNameValue = widgetName.getValue(ctx);
                }
                if (StringUtils.isBlank(widgetNameValue)) {
                    widgetNameValue = actionInstance.getId();
                }
                // avoid double markers
                if (widgetNameValue != null && widgetNameValue.startsWith(FaceletHandlerHelper.WIDGET_ID_PREFIX)) {
                    widgetNameValue = widgetNameValue.substring(FaceletHandlerHelper.WIDGET_ID_PREFIX.length());
                }

                WidgetDefinitionImpl wDef = new WidgetDefinitionImpl(widgetNameValue, wtype, actionInstance.getLabel(),
                        actionInstance.getHelp(), true, null, null, props, null);
                wDef.setTypeCategory(wcat);
                wDef.setDynamic(true);
                WebLayoutManager layoutService = Framework.getService(WebLayoutManager.class);
                Widget widgetInstance = layoutService.createWidget(ctx, wDef, modeValue, bareValueName, null);
                if (widgetInstance != null) {
                    // set unique id on widget before exposing it to the context
                    FaceletHandlerHelper helper = new FaceletHandlerHelper(config);
                    WidgetTagHandler.generateWidgetId(ctx, helper, widgetInstance, false);

                    // expose widget variables
                    WidgetTagHandler.exposeWidgetVariables(ctx, vm, widgetInstance, null, false);

                    // create widget handler
                    TagAttributes wattrs = FaceletHandlerHelper.getTagAttributes();
                    wattrs = FaceletHandlerHelper.addTagAttribute(wattrs,
                            helper.createAttribute(RenderVariables.widgetVariables.widget.name(),
                                    "#{" + RenderVariables.widgetVariables.widget.name() + "}"));
                    wattrs = FaceletHandlerHelper.addTagAttribute(wattrs,
                            helper.createAttribute("value", value.getValue()));
                    TagConfig wconfig = TagConfigFactory.createTagConfig(config, config.getTagId(), wattrs,
                            nextHandler);
                    FaceletHandler handler = new WidgetTagHandler(wconfig);

                    // expose ajax render props to the context
                    String reRender = (String) props.get("ajaxReRender");
                    if (!StringUtils.isEmpty(reRender)) {
                        ExpressionFactory eFactory = ctx.getExpressionFactory();
                        ValueExpression ve = eFactory.createValueExpression(
                                "#{nxu:joinRender(ajaxReRender, " + reRender + ")}", String.class);
                        vm.setVariable("ajaxReRender", ve);
                    }

                    // create form handler if needed
                    boolean doAddForm = false;
                    if (addForm != null) {
                        doAddForm = addForm.getBoolean(ctx);
                    }
                    if (!doAddForm) {
                        // check if addForm information held by the action configuration
                        doAddForm = helper.createAttribute("addForm",
                                String.valueOf(widgetInstance.getProperty("addForm"))).getBoolean(ctx);
                    }
                    if (doAddForm) {
                        // resolve form related attributes early
                        boolean discard = helper.createAttribute("discardSurroundingForm",
                                String.valueOf(widgetInstance.getProperty("discardSurroundingForm"))).getBoolean(ctx);
                        boolean doUseAjaxForm = false;
                        if (useAjaxForm != null) {
                            doUseAjaxForm = useAjaxForm.getBoolean(ctx);
                        }
                        if (!discard || doUseAjaxForm) {
                            List<TagAttribute> fattrs = new ArrayList<>();
                            if (doUseAjaxForm) {
                                Object ajaxProp = widgetInstance.getProperty("ajaxSupport");
                                if (ajaxProp == null) {
                                    ajaxProp = widgetInstance.getProperty("supportAjax");
                                }
                                fattrs.add(helper.createAttribute("useAjaxForm", String.valueOf(ajaxProp)));
                            }
                            fattrs.add(helper.createAttribute("disableMultipartForm",
                                    String.valueOf(widgetInstance.getProperty("disableMultipartForm"))));
                            fattrs.add(helper.createAttribute("disableDoubleClickShield",
                                    String.valueOf(widgetInstance.getProperty("disableDoubleClickShield"))));
                            fattrs.add(helper.createAttribute("styleClass",
                                    formStyleClass != null ? formStyleClass.getValue() : null));
                            fattrs.add(helper.createAttribute("id", widgetInstance.getId() + "_form"));

                            TagConfig fconfig = TagConfigFactory.createTagConfig(config, config.getTagId(),
                                    new TagAttributesImpl(fattrs.toArray(new TagAttribute[] {})), handler);
                            handler = new FormTagHandler(fconfig);
                        }
                    }

                    handler.apply(ctx, parent);
                }

            } finally {
                ctx.setVariableMapper(orig);
            }

        }

    }

    @Override
    @SuppressWarnings("rawtypes")
    protected MetaRuleset createMetaRuleset(Class type) {
        return null;
    }

}