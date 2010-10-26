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
import javax.faces.FacesException;
import javax.faces.component.UIComponent;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.ui.web.binding.alias.AliasTagHandler;
import org.nuxeo.ecm.platform.ui.web.binding.alias.AliasVariableMapper;

import com.sun.facelets.FaceletContext;
import com.sun.facelets.FaceletException;
import com.sun.facelets.tag.TagAttribute;
import com.sun.facelets.tag.TagConfig;
import com.sun.facelets.tag.TagException;

/**
 * Tag handler that exposes a variable to the variable map. Behaviour is close
 * to the c:set tag handler except:
 * <ul>
 * <li>It allows caching a variable using cache parameter: variable will be
 * resolved the first time is is called and will be put in the context after</li>
 * <li>The resolved variable is removed from context when tag is closed to
 * avoid filling the context with it</li>
 * <li>Since 5.4, variables are made available in the request context after
 * the JSF component tree build thanks to a backing component.</li>
 * </ul>
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 * @since 5.3.1
 */
public class SetTagHandler extends AliasTagHandler {

    private static final Log log = LogFactory.getLog(AliasTagHandler.class);

    protected final TagAttribute var;

    protected final TagAttribute value;

    public SetTagHandler(TagConfig config) {
        super(config, null);
        var = getRequiredAttribute("var");
        value = getAttribute("value");
    }

    public void apply(FaceletContext ctx, UIComponent parent)
            throws IOException, FacesException, FaceletException, ELException {
        // make sure our parent is not null
        if (parent == null) {
            throw new TagException(this.tag, "Parent UIComponent was null");
        }

        // our id
        String id = ctx.generateUniqueId(this.tagId);

        // handle variable expression
        boolean cacheValue = false;
        if (cache != null) {
            cacheValue = cache.getBoolean(ctx);
        }
        String varStr = var.getValue(ctx);
        ValueExpression ve;
        if (cacheValue) {
            // resolve value and put it as is in variable mapper
            Object res = value.getObject(ctx);
            ve = ctx.getExpressionFactory().createValueExpression(res,
                    Object.class);
        } else {
            ve = value.getValueExpression(ctx, Object.class);
        }

        AliasVariableMapper target = new AliasVariableMapper(id);
        target.setVariable(varStr, ve);

        apply(ctx, parent, target);
    }

}
