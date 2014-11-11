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
import javax.faces.FacesException;
import javax.faces.component.UIComponent;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.ecm.platform.ui.web.binding.MetaValueExpression;
import org.nuxeo.ecm.platform.ui.web.binding.alias.AliasTagHandler;
import org.nuxeo.ecm.platform.ui.web.binding.alias.AliasVariableMapper;
import org.nuxeo.ecm.platform.ui.web.util.ComponentTagUtils;

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
 * <li>Since 5.4, variables are made available in the request context after the
 * JSF component tree build thanks to a backing component.</li>
 * </ul>
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 * @since 5.3.1
 */
public class SetTagHandler extends AliasTagHandler {

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

    public SetTagHandler(TagConfig config) {
        super(config, null);
        var = getRequiredAttribute("var");
        value = getAttribute("value");
        resolveTwice = getAttribute("resolveTwice");
        blockPatterns = getAttribute("blockPatterns");
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
        boolean resolveTwiceBool = false;
        if (resolveTwice != null) {
            resolveTwiceBool = resolveTwice.getBoolean(ctx);
        }

        String varStr = var.getValue(ctx);
        ValueExpression ve;
        if (cacheValue) {
            // resolve value and put it as is in variable mapper
            Object res = value.getObject(ctx);
            if (resolveTwiceBool && res instanceof String
                    && ComponentTagUtils.isValueReference((String) res)) {
                ve = ctx.getExpressionFactory().createValueExpression(ctx,
                        (String) res, Object.class);
                res = ve.getValue(ctx);
            }
            ve = ctx.getExpressionFactory().createValueExpression(res,
                    Object.class);
        } else {
            ve = value.getValueExpression(ctx, Object.class);
            if (resolveTwiceBool) {
                ve = new MetaValueExpression(ve);
            }
        }

        AliasVariableMapper target = new AliasVariableMapper(id);
        target.setVariable(varStr, ve);

        if (blockPatterns != null) {
            String blocked = blockPatterns.getValue(ctx);
            if (!StringUtils.isEmpty(blocked)) {
                // split on "," character
                target.setBlockedPatterns(resolveBlockPatterns(blocked));
            }
        }

        apply(ctx, parent, target);
    }

    protected List<String> resolveBlockPatterns(String value) {
        List<String> res = new ArrayList<String>();
        if (value != null) {
            if (value.contains(",")) {
                // parse potential multiple patterns
                String[] names = value.split(",");
                for (String name : names) {
                    if (!StringUtils.isBlank(name)) {
                        name = name.trim();
                        res.add(name);
                    }
                }
            } else {
                value = value.trim();
                res.add(value);
            }
        }
        return res;
    }

}
