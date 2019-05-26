/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.platform.ui.web.binding.alias;

import java.io.IOException;

import javax.el.ELException;
import javax.el.ExpressionFactory;
import javax.el.ValueExpression;
import javax.el.VariableMapper;
import javax.faces.FacesException;
import javax.faces.component.UIComponent;
import javax.faces.view.facelets.ComponentConfig;
import javax.faces.view.facelets.FaceletContext;

import org.nuxeo.ecm.platform.ui.web.tag.handler.GenericHtmlComponentHandler;

import com.sun.faces.facelets.el.VariableMapperWrapper;

/**
 * Generic HTML component handler ensuring that sub handlers extending {@link AliasTagHandler} will anchor their
 * components in the tree.
 * <p>
 * This is helpful when exposing variables depending on a variable that is only available at render time (like a list or
 * dataTable component), to ensure accurate resolution of variables at render time.
 *
 * @since 6.0
 * @see AliasTagHandler
 */
public class AliasAnchorComponentHandler extends GenericHtmlComponentHandler {

    public AliasAnchorComponentHandler(ComponentConfig config) {
        super(config);
    }

    @Override
    public void applyNextHandler(FaceletContext ctx, UIComponent c) throws IOException, FacesException, ELException {
        VariableMapper orig = ctx.getVariableMapper();
        VariableMapper vm = new VariableMapperWrapper(orig);
        ExpressionFactory eFactory = ctx.getExpressionFactory();
        ValueExpression ve = eFactory.createValueExpression(Boolean.TRUE, Boolean.class);
        vm.setVariable(AliasTagHandler.ANCHOR_ENABLED_VARIABLE, ve);
        ctx.setVariableMapper(vm);
        try {
            super.applyNextHandler(ctx, c);
        } finally {
            ctx.setVariableMapper(orig);
        }
    }

}
