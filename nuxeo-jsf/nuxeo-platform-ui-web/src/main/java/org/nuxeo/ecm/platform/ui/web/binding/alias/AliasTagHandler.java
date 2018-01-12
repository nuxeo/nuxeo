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
 *     Nuxeo - initial API and implementation
 *
 * $Id: $
 */

package org.nuxeo.ecm.platform.ui.web.binding.alias;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.el.ELException;
import javax.el.ExpressionFactory;
import javax.el.ValueExpression;
import javax.el.VariableMapper;
import javax.faces.FacesException;
import javax.faces.component.UIComponent;
import javax.faces.component.UIViewRoot;
import javax.faces.context.FacesContext;
import javax.faces.view.facelets.ComponentConfig;
import javax.faces.view.facelets.ComponentHandler;
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.FaceletException;
import javax.faces.view.facelets.FaceletHandler;
import javax.faces.view.facelets.TagAttribute;
import javax.faces.view.facelets.TagException;

import org.nuxeo.ecm.platform.ui.web.binding.BlockingVariableMapper;
import org.nuxeo.ecm.platform.ui.web.util.FaceletDebugTracer;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.services.config.ConfigurationService;

import com.sun.faces.facelets.tag.jsf.ComponentSupport;

/**
 * Tag handler that exposes variables to the variable map. Behaviour is close to the c:set tag handler except:
 * <ul>
 * <li>It handles several variables</li>
 * <li>It allows caching a variable using cache parameter: variable will be resolved the first time is is called and
 * will be put in the context after</li>
 * <li>The resolved variable is removed from context when tag is closed to avoid filling the context with it</li>
 * <li>Variables are made available in the request context after the JSF component tree build thanks to a backing
 * component.</li>
 * </ul>
 * <p>
 * The backing component value expressions are changed even if the component was found to ensure a good resolution even
 * when re-rendering the tag using ajax.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 * @since 5.4
 */
public class AliasTagHandler extends ComponentHandler {

    /**
     * @since 6.0
     */
    public static final String ANCHOR_ENABLED_VARIABLE = "nuxeoAliasAnchorEnabled";

    protected final TagAttribute cache;

    protected final TagAttribute id;

    protected final Map<String, ValueExpression> variables;

    protected final List<String> blockedPatterns;

    /**
     * @since 6.0
     */
    protected final TagAttribute anchor;

    public AliasTagHandler(ComponentConfig config, Map<String, ValueExpression> variables) {
        this(config, variables, null);
    }

    /**
     * @since 5.6
     */
    public AliasTagHandler(ComponentConfig config, Map<String, ValueExpression> variables,
            List<String> blockedPatterns) {
        super(config);
        id = getAttribute("id");
        cache = getAttribute("cache");
        anchor = getAttribute("anchor");
        this.variables = variables;
        this.blockedPatterns = blockedPatterns;
    }

    @Override
    public void apply(FaceletContext ctx, UIComponent parent)
            throws IOException, FacesException, FaceletException, ELException {
        long start = FaceletDebugTracer.start();
        try {
            // make sure our parent is not null
            if (parent == null) {
                throw new TagException(tag, "Parent UIComponent was null");
            }

            // handle variable expression
            boolean cacheValue = false;
            if (cache != null) {
                cacheValue = cache.getBoolean(ctx);
            }

            if (isOptimizedAgain()) {
                applyOptimized(ctx, parent, cacheValue);
            } else {
                applyAlias(ctx, parent, cacheValue);
            }

        } finally {
            FaceletDebugTracer.trace(start, getTag(), "alias");
        }
    }

    protected boolean isOptimizedAgain() {
        ConfigurationService cs = Framework.getService(ConfigurationService.class);
        return !cs.isBooleanPropertyTrue("nuxeo.jsf.removeAliasOptimsReloaded");
    }

    protected void applyOptimized(FaceletContext ctx, UIComponent parent, boolean cache)
            throws IOException, FacesException, FaceletException, ELException {
        VariableMapper orig = ctx.getVariableMapper();
        try {
            BlockingVariableMapper vm = new BlockingVariableMapper(orig);
            vm.setBlockedPatterns(blockedPatterns);
            if (variables != null) {
                for (Map.Entry<String, ValueExpression> var : variables.entrySet()) {
                    if (cache) {
                        // resolve value and put it as is in variables
                        Object res = var.getValue().getValue(ctx);
                        ValueExpression ve = ctx.getExpressionFactory().createValueExpression(res, Object.class);
                        vm.setVariable(var.getKey(), ve);
                    } else {
                        vm.setVariable(var.getKey(), var.getValue());
                    }
                }
            }
            ctx.setVariableMapper(vm);
            nextHandler.apply(ctx, parent);
        } finally {
            ctx.setVariableMapper(orig);
        }
    }

    protected void applyAlias(FaceletContext ctx, UIComponent parent, boolean cacheValue)
            throws IOException, FacesException, FaceletException, ELException {
        AliasVariableMapper target = new AliasVariableMapper();
        target.setBlockedPatterns(blockedPatterns);
        if (variables != null) {
            for (Map.Entry<String, ValueExpression> var : variables.entrySet()) {
                if (cacheValue) {
                    // resolve value and put it as is in variables
                    Object res = var.getValue().getValue(ctx);
                    target.setVariable(var.getKey(),
                            ctx.getExpressionFactory().createValueExpression(res, Object.class));
                } else {
                    target.setVariable(var.getKey(), var.getValue());
                }
            }
        }

        // generate id before applying
        target.setId(ctx.generateUniqueId(tagId));
        apply(ctx, parent, target, nextHandler);
    }

    protected void apply(FaceletContext ctx, UIComponent parent, AliasVariableMapper alias, FaceletHandler nextHandler)
            throws IOException, FacesException, FaceletException, ELException {
        ConfigurationService configurationService = Framework.getService(ConfigurationService.class);
        if (configurationService.isBooleanPropertyTrue("nuxeo.jsf.removeAliasOptims")) {
            applyCompat(ctx, parent, alias, nextHandler);
            return;
        }

        applyAliasHandler(ctx, parent, alias, nextHandler);
    }

    protected boolean isAnchored(FaceletContext ctx) {
        if (cache != null && cache.getBoolean(ctx)) {
            return false;
        }
        ExpressionFactory eFactory = ctx.getExpressionFactory();
        ValueExpression ve = eFactory.createValueExpression(ctx, "#{" + ANCHOR_ENABLED_VARIABLE + "}", Boolean.class);
        if (Boolean.TRUE.equals(ve.getValue(ctx))) {
            return true;
        }
        if (anchor != null) {
            return anchor.getBoolean(ctx);
        }
        return false;
    }

    protected void applyAliasHandler(FaceletContext ctx, UIComponent parent, AliasVariableMapper alias,
            FaceletHandler nextHandler) throws IOException, FacesException, FaceletException, ELException {
        // resolve the "anchor" attribute to decide whether variable should be
        // anchored in the tree as a UIAliasHolder
        boolean createComponent = isAnchored(ctx);
        if (createComponent) {
            // start by removing component from tree if it is already there, to
            // make sure it's recreated next
            String id = ctx.generateUniqueId(getTagId());
            UIComponent c = ComponentSupport.findChildByTagId(parent, id);
            if (c != null && c.getParent() != parent) {
                c.getParent().getChildren().remove(c);
            }
        }

        String id = alias.getId();
        VariableMapper orig = ctx.getVariableMapper();
        VariableMapper vm = alias.getVariableMapperForBuild(orig);
        ctx.setVariableMapper(vm);
        FacesContext facesContext = ctx.getFacesContext();
        try {
            AliasVariableMapper.exposeAliasesToRequest(facesContext, alias);
            if (createComponent) {
                super.apply(ctx, parent);
            } else {
                nextHandler.apply(ctx, parent);
            }
        } finally {
            AliasVariableMapper.removeAliasesExposedToRequest(facesContext, id);
            ctx.setVariableMapper(orig);
        }
    }

    /**
     * Compatibility application of facelet handler, used to preserve behaviour while optimizing and improving variables
     * exposure and resolution.
     */
    protected void applyCompat(FaceletContext ctx, UIComponent parent, AliasVariableMapper alias,
            FaceletHandler nextHandler) throws IOException, FacesException, FaceletException, ELException {
        String id = alias.getId();

        VariableMapper orig = ctx.getVariableMapper();
        VariableMapper vm = alias.getVariableMapperForBuild(orig);
        ctx.setVariableMapper(vm);

        // create component
        UIComponent c = ComponentSupport.findChildByTagId(parent, id);
        boolean componentFound = false;
        if (c != null) {
            componentFound = true;
            // mark all children for cleaning
            ComponentSupport.markForDeletion(c);
        } else {
            c = new UIAliasHolder();

            // mark it owned by a facelet instance
            c.getAttributes().put(ComponentSupport.MARK_CREATED, id);

            // assign our unique id
            if (this.id != null) {
                c.setId(this.id.getValue(ctx));
            } else {
                UIViewRoot root = ComponentSupport.getViewRoot(ctx, parent);
                if (root != null) {
                    String uid = root.createUniqueId();
                    c.setId(uid);
                }
            }
        }

        // update value held by component
        ((UIAliasHolder) c).setAlias(alias);

        FacesContext facesContext = ctx.getFacesContext();
        try {
            AliasVariableMapper.exposeAliasesToRequest(facesContext, alias);
            // first allow c to get populated
            nextHandler.apply(ctx, c);
        } finally {
            AliasVariableMapper.removeAliasesExposedToRequest(facesContext, id);
            ctx.setVariableMapper(orig);
        }

        // finish cleaning up orphaned children
        if (componentFound) {
            ComponentSupport.finalizeForDeletion(c);
        }

        // add to the tree afterwards
        // this allows children to determine if it's
        // been part of the tree or not yet
        parent.getChildren().add(c);
    }

}
