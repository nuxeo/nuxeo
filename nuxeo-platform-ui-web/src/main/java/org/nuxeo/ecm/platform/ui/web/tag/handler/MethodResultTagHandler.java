/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

    private static final Class[] DEFAULT_PARAM_TYPES_CLASSES = new Class[0];

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

    private Class[] resolveParamTypes(FaceletContext ctx) {
        // TODO: implement string parsing ?
        return DEFAULT_PARAM_TYPES_CLASSES;
    }

    public void apply(FaceletContext ctx, UIComponent parent)
            throws IOException {
        String nameStr = name.getValue(ctx);
        // resolve given value as a method binding, paramtypes ignored for now
        Class[] paramTypesClasses = resolveParamTypes(ctx);
        MethodExpression meth = value.getMethodExpression(ctx, Object.class,
                paramTypesClasses);
        Boolean invokeNow = false;
        if (immediate != null) {
            invokeNow = immediate.getBoolean(ctx);
        }
        ValueExpression ve;
        if (invokeNow) {
            Object res = meth.invoke(ctx, paramTypesClasses);
            ve = ctx.getExpressionFactory().createValueExpression(res,
                    Object.class);
        } else {
            ve = new MethodValueExpression(ctx.getFunctionMapper(),
                    ctx.getVariableMapper(), meth, paramTypesClasses);
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
    protected MetaRuleset createMetaRuleset(Class type) {
        return null;
    }

}
