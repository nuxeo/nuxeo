/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
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
package org.nuxeo.ecm.platform.ui.web.tag.handler;

import java.io.IOException;

import javax.el.ValueExpression;
import javax.faces.component.UIComponent;
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.TagAttribute;
import javax.faces.view.facelets.TagConfig;

import org.nuxeo.ecm.platform.ui.web.binding.MetaValueExpression;

import com.sun.faces.facelets.tag.ui.DecorateHandler;

/**
 * Tag similar to {@link com.sun.faces.facelets.tag.ui.ParamHandler}, except it can resolve expressions twice.
 * <p>
 * Extends {@link com.sun.faces.facelets.tag.ui.ParamHandler} for accurate discovery inside a {@link DecorateHandler}
 * tag for instance.
 *
 * @since 7.4
 */
public class ParamHandler extends com.sun.faces.facelets.tag.ui.ParamHandler {

    protected final TagAttribute name;

    protected final TagAttribute value;

    protected final TagAttribute resolveTwice;

    public ParamHandler(TagConfig config) {
        super(config);
        name = getRequiredAttribute("name");
        value = getRequiredAttribute("value");
        resolveTwice = getAttribute("resolveTwice");
    }

    public void apply(FaceletContext ctx, UIComponent parent) throws IOException {
        String nameStr = name.getValue(ctx);
        boolean resolveTwiceBool = false;
        if (resolveTwice != null) {
            resolveTwiceBool = resolveTwice.getBoolean(ctx);
        }
        ValueExpression ve = value.getValueExpression(ctx, Object.class);
        if (resolveTwiceBool) {
            ve = new MetaValueExpression(ve, ctx.getFunctionMapper(), ctx.getVariableMapper());
        }
        ctx.getVariableMapper().setVariable(nameStr, ve);
    }

}