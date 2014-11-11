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
 * Meta rule set that wires a method binding to a {@link MetaMethodBinding} when
 * invoking the method.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public class MetaActionSourceRule extends MetaRule {

    public static final Class[] ACTION_SIG = new Class[0];

    public static final MetaActionSourceRule Instance = new MetaActionSourceRule();

    static final class ActionExpressionMapper extends Metadata {

        private final TagAttribute attr;

        ActionExpressionMapper(TagAttribute attr) {
            this.attr = attr;
        }

        @Override
        public void applyMetadata(FaceletContext ctx, Object instance) {
            ActionSource2 as = (ActionSource2) instance;
            MethodExpression originalExpression = attr.getMethodExpression(ctx,
                    String.class, ACTION_SIG);
            as.setActionExpression(new MetaMethodExpression(originalExpression));
        }
    }

    @Override
    public Metadata applyRule(String name, TagAttribute attribute,
            MetadataTarget meta) {
        if (meta.isTargetInstanceOf(ActionSource.class)) {
            if ("action".equals(name)) {
                return new ActionExpressionMapper(attribute);
            }
        }
        return null;
    }
}
