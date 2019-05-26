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
 * $Id: MetaActionSourceRule.java 21703 2007-07-01 20:48:16Z sfermigier $
 */

package org.nuxeo.ecm.platform.ui.web.tag.jsf;

import javax.el.MethodExpression;
import javax.faces.component.ActionSource;
import javax.faces.component.ActionSource2;
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.MetaRule;
import javax.faces.view.facelets.Metadata;
import javax.faces.view.facelets.MetadataTarget;
import javax.faces.view.facelets.TagAttribute;

import org.nuxeo.ecm.platform.ui.web.binding.MetaMethodExpression;

/**
 * Meta rule set that wires a method binding to a {@link MetaMethodBinding} when invoking the method.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public class MetaActionSourceRule extends MetaRule {

    public static final Class<?>[] ACTION_SIG = new Class[0];

    public static final MetaActionSourceRule Instance = new MetaActionSourceRule();

    static final class ActionExpressionMapper extends Metadata {

        private final TagAttribute attr;

        ActionExpressionMapper(TagAttribute attr) {
            this.attr = attr;
        }

        @Override
        public void applyMetadata(FaceletContext ctx, Object instance) {
            ActionSource2 as = (ActionSource2) instance;
            MethodExpression originalExpression = attr.getMethodExpression(ctx, String.class, ACTION_SIG);
            as.setActionExpression(
                    new MetaMethodExpression(originalExpression, ctx.getFunctionMapper(), ctx.getVariableMapper()));
        }
    }

    @Override
    public Metadata applyRule(String name, TagAttribute attribute, MetadataTarget meta) {
        if (meta.isTargetInstanceOf(ActionSource.class)) {
            if ("action".equals(name)) {
                return new ActionExpressionMapper(attribute);
            }
        }
        return null;
    }
}
