/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: $
 */

package org.nuxeo.ecm.platform.ui.web.tag.handler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.el.ELException;
import javax.el.ValueExpression;
import javax.el.VariableMapper;
import javax.faces.FacesException;
import javax.faces.component.UIComponent;
import javax.faces.view.facelets.ComponentConfig;
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.FaceletException;
import javax.faces.view.facelets.FaceletHandler;
import javax.faces.view.facelets.TagAttribute;
import javax.faces.view.facelets.TagException;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.ui.web.binding.BlockingVariableMapper;
import org.nuxeo.ecm.platform.ui.web.binding.MetaValueExpression;
import org.nuxeo.ecm.platform.ui.web.binding.alias.AliasTagHandler;
import org.nuxeo.ecm.platform.ui.web.binding.alias.AliasVariableMapper;
import org.nuxeo.ecm.platform.ui.web.util.ComponentTagUtils;
import org.nuxeo.ecm.platform.ui.web.util.FaceletDebugTracer;

/**
 * Tag handler that exposes a variable to the variable map. Behaviour is close to the c:set tag handler except:
 * <ul>
 * <li>It allows caching a variable using cache parameter: variable will be resolved the first time is is called and
 * will be put in the context after</li>
 * <li>The resolved variable is removed from context when tag is closed to avoid filling the context with it</li>
 * <li>Since 5.4, variables are made available in the request context after the JSF component tree build thanks to a
 * backing component.</li>
 * </ul>
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 * @since 5.3.1
 */
public class SetTagHandler extends AliasTagHandler {

    private static final Log log = LogFactory.getLog(SetTagHandler.class);

    protected final TagAttribute var;

    protected final TagAttribute value;

    /**
     * @since 5.5
     */
    protected final TagAttribute resolveTwice;

    /**
     * @since 5.6
     */
    protected final TagAttribute blockPatterns;

    /**
     * @since 5.9.2
     */
    protected final TagAttribute blockMerge;

    /**
     * Determines in which context expression will be evaluated when expression is not cached and resolved twice (build
     * time by default, render time if local).
     *
     * @since 7.10
     */
    protected final TagAttribute local;

    /**
     * Force using of {@link AliasVariableMapper} logics, exposing a reference of to a value that might change between
     * "restore view" and "render response" phase, to make sure it's not cached by components and resolved again at
     * "render response" phase.
     *
     * @since 8.2
     */
    protected final TagAttribute useAlias;

    public SetTagHandler(ComponentConfig config) {
        super(config, null);
        var = getRequiredAttribute("var");
        value = getAttribute("value");
        resolveTwice = getAttribute("resolveTwice");
        blockPatterns = getAttribute("blockPatterns");
        blockMerge = getAttribute("blockMerge");
        local = getAttribute("local");
        useAlias = getAttribute("useAlias");
    }

    @Override
    public void apply(FaceletContext ctx, UIComponent parent)
            throws IOException, FacesException, FaceletException, ELException {
        long start = FaceletDebugTracer.start();
        String varStr = null;
        try {

            // make sure our parent is not null
            if (parent == null) {
                throw new TagException(tag, "Parent UIComponent was null");
            }

            boolean useAliasBool = false;
            if (useAlias != null) {
                useAliasBool = useAlias.getBoolean(ctx);
            }

            if (!useAliasBool && isOptimizedAgain()) {
                varStr = var.getValue(ctx);
                VariableMapper orig = ctx.getVariableMapper();
                boolean done = false;
                if (orig instanceof BlockingVariableMapper) {
                    BlockingVariableMapper vm = (BlockingVariableMapper) orig;
                    if (isAcceptingMerge(ctx, vm, varStr)) {
                        FaceletHandler next = applyOptimized(ctx, parent, vm, varStr);
                        next.apply(ctx, parent);
                        done = true;
                    }
                }
                if (!done) {
                    try {
                        BlockingVariableMapper vm = new BlockingVariableMapper(orig);
                        ctx.setVariableMapper(vm);
                        FaceletHandler next = applyOptimized(ctx, parent, vm, varStr);
                        next.apply(ctx, parent);
                    } finally {
                        ctx.setVariableMapper(orig);
                    }
                }
            } else {
                applyAlias(ctx, parent);
            }
        } finally {
            FaceletDebugTracer.trace(start, getTag(), var.getValue());
        }
    }

    public FaceletHandler getNextHandler() {
        return nextHandler;
    }

    public boolean isAcceptingMerge(FaceletContext ctx, BlockingVariableMapper vm, String var) {
        // avoid overriding variable already in the mapper
        if (vm.hasVariable(var)) {
            return false;
        }
        return isAcceptingMerge(ctx);
    }

    public boolean isAcceptingMerge(FaceletContext ctx) {
        if (useAlias != null && useAlias.getBoolean(ctx)) {
            return false;
        }
        if (blockMerge != null && blockMerge.getBoolean(ctx)) {
            return false;
        }
        if (blockPatterns != null) {
            String blocked = blockPatterns.getValue(ctx);
            if (!StringUtils.isEmpty(blocked)) {
                return false;
            }
        }
        return true;
    }

    public FaceletHandler applyOptimized(FaceletContext ctx, UIComponent parent, BlockingVariableMapper vm)
            throws IOException {
        String varStr = var.getValue(ctx);
        return applyOptimized(ctx, parent, vm, varStr);
    }

    public FaceletHandler applyOptimized(FaceletContext ctx, UIComponent parent, BlockingVariableMapper vm,
            String varStr) throws IOException {

        // handle variable expression
        boolean cacheValue = false;
        if (cache != null) {
            cacheValue = cache.getBoolean(ctx);
        }
        boolean resolveTwiceBool = false;
        if (resolveTwice != null) {
            resolveTwiceBool = resolveTwice.getBoolean(ctx);
        }

        ValueExpression ve;
        if (cacheValue) {
            // resolve value and put it as is in variable mapper
            Object res = value.getObject(ctx);
            if (resolveTwiceBool && res instanceof String && ComponentTagUtils.isValueReference((String) res)) {
                ve = ctx.getExpressionFactory().createValueExpression(ctx, (String) res, Object.class);
                res = ve.getValue(ctx);
            }
            ve = ctx.getExpressionFactory().createValueExpression(res, Object.class);
        } else {
            ve = value.getValueExpression(ctx, Object.class);
            if (resolveTwiceBool) {
                boolean localBool = false;
                if (local != null) {
                    localBool = local.getBoolean(ctx);
                }
                if (localBool) {
                    ve = new MetaValueExpression(ve);
                } else {
                    ve = new MetaValueExpression(ve, ctx.getFunctionMapper(), vm);
                }
            }
        }

        vm.setVariable(varStr, ve);

        if (blockPatterns != null) {
            String blockedValue = blockPatterns.getValue(ctx);
            if (!StringUtils.isEmpty(blockedValue)) {
                // split on "," character
                vm.setBlockedPatterns(resolveBlockPatterns(blockedValue));
            }
        }

        FaceletHandler nextHandler = this.nextHandler;
        if (nextHandler instanceof SetTagHandler) {
            // try merging with next handler
            SetTagHandler next = (SetTagHandler) nextHandler;
            if (next.isAcceptingMerge(ctx)) {
                nextHandler = next.applyOptimized(ctx, parent, vm);
            }
        }

        return nextHandler;
    }

    public void applyAlias(FaceletContext ctx, UIComponent parent) throws IOException {
        FaceletHandler nextHandler = this.nextHandler;
        VariableMapper orig = ctx.getVariableMapper();
        AliasVariableMapper target = new AliasVariableMapper();
        // generate id before applying (and before generating next handler, in
        // case of merge of variables, as parent aliases will be exposed to
        // request then).
        target.setId(ctx.generateUniqueId(tagId));

        VariableMapper vm = target.getVariableMapperForBuild(orig);
        ctx.setVariableMapper(vm);
        try {
            nextHandler = getAliasVariableMapper(ctx, target);
        } finally {
            ctx.setVariableMapper(orig);
        }
        applyAliasHandler(ctx, parent, target, nextHandler);
    }

    public FaceletHandler getAliasVariableMapper(FaceletContext ctx, AliasVariableMapper target) {
        String varStr = var.getValue(ctx);
        // avoid overriding variable already in the mapper
        if (target.hasVariables(varStr)) {
            return nextHandler;
        }

        // handle variable expression
        boolean cacheValue = false;
        if (cache != null) {
            cacheValue = cache.getBoolean(ctx);
        }
        boolean resolveTwiceBool = false;
        if (resolveTwice != null) {
            resolveTwiceBool = resolveTwice.getBoolean(ctx);
        }

        ValueExpression ve;
        if (cacheValue) {
            // resolve value and put it as is in variable mapper
            Object res = value.getObject(ctx);
            if (resolveTwiceBool && res instanceof String && ComponentTagUtils.isValueReference((String) res)) {
                ve = ctx.getExpressionFactory().createValueExpression(ctx, (String) res, Object.class);
                res = ve.getValue(ctx);
            }
            ve = ctx.getExpressionFactory().createValueExpression(res, Object.class);
        } else {
            ve = value.getValueExpression(ctx, Object.class);
            if (resolveTwiceBool) {
                boolean localBool = false;
                if (local != null) {
                    localBool = local.getBoolean(ctx);
                }
                if (localBool) {
                    ve = new MetaValueExpression(ve);
                } else {
                    ve = new MetaValueExpression(ve, ctx.getFunctionMapper(), ctx.getVariableMapper());
                }
            }
        }

        target.setVariable(varStr, ve);

        if (blockPatterns != null) {
            String blockedValue = blockPatterns.getValue(ctx);
            if (!StringUtils.isEmpty(blockedValue)) {
                // split on "," character
                target.setBlockedPatterns(resolveBlockPatterns(blockedValue));
            }
        }

        FaceletHandler nextHandler = this.nextHandler;
        if (nextHandler instanceof SetTagHandler) {
            // try merging with next handler
            SetTagHandler next = (SetTagHandler) nextHandler;
            if (next.isAcceptingMerge(ctx)) {
                // make sure referenced vars will be resolved in this context
                ctx.getVariableMapper().setVariable(varStr, ve);
                try {
                    AliasVariableMapper.exposeAliasesToRequest(ctx.getFacesContext(), target);
                    nextHandler = next.getAliasVariableMapper(ctx, target);
                } finally {
                    AliasVariableMapper.removeAliasesExposedToRequest(ctx.getFacesContext(), target.getId());
                }
            }
        }

        return nextHandler;
    }

    protected List<String> resolveBlockPatterns(String value) {
        List<String> res = new ArrayList<String>();
        if (value != null) {
            String[] split = StringUtils.split(value, ',');
            if (split != null) {
                for (String item : split) {
                    res.add(item.trim());
                }
            }
        }
        return res;
    }

}
