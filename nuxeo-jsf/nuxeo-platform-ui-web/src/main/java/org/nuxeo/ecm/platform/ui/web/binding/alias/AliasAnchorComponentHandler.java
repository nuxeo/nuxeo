/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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
 * Generic HTML component handler that ensures that {@link AliasTagHandler}
 * usage within this handler will make sure components are anchored in the
 * tree.
 * <p>
 * This is helpful when expôsing variables depending on a variabled only
 * available at render time (like a list or dataTable component), to ensure
 * accurate resolution of variables at render time.
 *
 * @since 6.0
 * @see AliasTagHandler
 */
public class AliasAnchorComponentHandler extends
        GenericHtmlComponentHandler {

    public AliasAnchorComponentHandler(ComponentConfig config) {
        super(config);
    }

    @Override
    public void applyNextHandler(FaceletContext ctx, UIComponent c)
            throws IOException, FacesException, ELException {
        VariableMapper orig = ctx.getVariableMapper();
        VariableMapper vm = new VariableMapperWrapper(orig);
        ExpressionFactory eFactory = ctx.getExpressionFactory();
        ValueExpression ve = eFactory.createValueExpression(Boolean.TRUE,
                Boolean.class);
        vm.setVariable(AliasTagHandler.ANCHOR_ENABLED_VARIABLE, ve);
        ctx.setVariableMapper(vm);
        try {
            super.applyNextHandler(ctx, c);
        } finally {
            ctx.setVariableMapper(orig);
        }
    }

}