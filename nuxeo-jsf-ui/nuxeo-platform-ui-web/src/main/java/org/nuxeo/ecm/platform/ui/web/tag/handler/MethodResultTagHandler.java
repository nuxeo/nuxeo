/*
 * (C) Copyright 2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 * $Id: MethodResultTagHandler.java 19474 2007-05-27 10:18:21Z sfermigier $
 */

package org.nuxeo.ecm.platform.ui.web.tag.handler;

import java.io.IOException;

import javax.el.MethodExpression;
import javax.el.ValueExpression;
import javax.el.VariableMapper;
import javax.faces.component.UIComponent;
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.MetaRuleset;
import javax.faces.view.facelets.MetaTagHandler;
import javax.faces.view.facelets.TagAttribute;
import javax.faces.view.facelets.TagConfig;

import org.nuxeo.ecm.platform.ui.web.binding.MethodValueExpression;

import com.sun.faces.facelets.el.VariableMapperWrapper;

/**
 * Tag handler that exposes the result of a method binding in the variable map.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 * @deprecated since Seam 2.0 handles method results
 * @see SetTagHandler for caching features using parameter immediate
 */
@Deprecated
public class MethodResultTagHandler extends MetaTagHandler {

    private static final Class<?>[] DEFAULT_PARAM_TYPES_CLASSES = new Class[0];

    private final TagAttribute name;

    private final TagAttribute value;

    private final TagAttribute immediate;

    private final TagAttribute paramTypes;

    public MethodResultTagHandler(TagConfig config) {
        super(config);
        name = getRequiredAttribute("name");
        value = getRequiredAttribute("value");
        immediate = getAttribute("immediate");
        paramTypes = getAttribute("paramTypes");
    }

    private Class<?>[] resolveParamTypes(FaceletContext ctx) {
        // TODO: implement string parsing ?
        return DEFAULT_PARAM_TYPES_CLASSES;
    }

    @Override
    public void apply(FaceletContext ctx, UIComponent parent) throws IOException {
        String nameStr = name.getValue(ctx);
        // resolve given value as a method binding, paramtypes ignored for now
        Class<?>[] paramTypesClasses = resolveParamTypes(ctx);
        MethodExpression meth = value.getMethodExpression(ctx, Object.class, paramTypesClasses);
        Boolean invokeNow = false;
        if (immediate != null) {
            invokeNow = immediate.getBoolean(ctx);
        }
        ValueExpression ve;
        if (invokeNow) {
            Object res = meth.invoke(ctx, paramTypesClasses);
            ve = ctx.getExpressionFactory().createValueExpression(res, Object.class);
        } else {
            ve = new MethodValueExpression(ctx.getFunctionMapper(), ctx.getVariableMapper(), meth, paramTypesClasses);
        }
        VariableMapper orig = ctx.getVariableMapper();
        VariableMapper vm = new VariableMapperWrapper(orig);
        ctx.setVariableMapper(vm);
        vm.setVariable(nameStr, ve);
        try {
            nextHandler.apply(ctx, parent);
        } finally {
            ctx.setVariableMapper(orig);
        }
    }

    @Override
    @SuppressWarnings("rawtypes")
    protected MetaRuleset createMetaRuleset(Class type) {
        return null;
    }

}
