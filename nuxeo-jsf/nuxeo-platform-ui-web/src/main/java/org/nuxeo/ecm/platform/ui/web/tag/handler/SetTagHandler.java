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

import javax.el.ELException;
import javax.el.ValueExpression;
import javax.el.VariableMapper;
import javax.faces.FacesException;
import javax.faces.component.UIComponent;

import com.sun.facelets.FaceletContext;
import com.sun.facelets.FaceletException;
import com.sun.facelets.el.VariableMapperWrapper;
import com.sun.facelets.tag.MetaTagHandler;
import com.sun.facelets.tag.TagAttribute;
import com.sun.facelets.tag.TagConfig;

/**
 * Tag handler that exposes a variable to the variable map. Behaviour is close
 * to the c:set tag handler except:
 * <ul>
 * <li>It allows caching a variable using cache parameter: variable will be
 * resolved the first time is is called and will be put in the context after</li>
 * <li>The resolved variable is removed from context when tag is closed to
 * avoid filling the context with it</li>
 * </ul>
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 * @since 5.3.1
 */
public class SetTagHandler extends MetaTagHandler {

    private final TagAttribute var;

    private final TagAttribute value;

    private final TagAttribute cache;

    public SetTagHandler(TagConfig config) {
        super(config);
        value = getRequiredAttribute("value");
        var = getRequiredAttribute("var");
        cache = getAttribute("cache");
    }

    public void apply(FaceletContext ctx, UIComponent parent)
            throws IOException, FacesException, FaceletException, ELException {
        String varStr = var.getValue(ctx);
        boolean cacheValue = false;
        if (cache != null) {
            cacheValue = cache.getBoolean(ctx);
        }

        ValueExpression ve = value.getValueExpression(ctx, Object.class);
        if (cacheValue) {
            // resolve value and put it as is in variable mapper
            Object res = ve.getValue(ctx);
            ve = ctx.getExpressionFactory().createValueExpression(res,
                    Object.class);
        }

        VariableMapper orig = ctx.getVariableMapper();
        VariableMapper vm = new VariableMapperWrapper(orig);
        ctx.setVariableMapper(vm);
        vm.setVariable(varStr, ve);
        try {
            nextHandler.apply(ctx, parent);
        } finally {
            ctx.setVariableMapper(orig);
        }
    }

}
