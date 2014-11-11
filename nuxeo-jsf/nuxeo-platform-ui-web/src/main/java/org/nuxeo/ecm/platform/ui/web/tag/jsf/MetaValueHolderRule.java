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
 * $Id: MetaValueHolderRule.java 28460 2008-01-03 15:34:05Z sfermigier $
 */

package org.nuxeo.ecm.platform.ui.web.tag.jsf;

import javax.el.ValueExpression;
import javax.faces.component.UIComponent;
import javax.faces.component.ValueHolder;
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.MetaRule;
import javax.faces.view.facelets.Metadata;
import javax.faces.view.facelets.MetadataTarget;
import javax.faces.view.facelets.TagAttribute;

import org.nuxeo.ecm.platform.ui.web.binding.MetaValueExpression;

/**
 * Meta value rule, used to evaluate an expression as a regular value
 * expression, or invoking it again as another value expression or method
 * expression.
 * <p>
 * The method can have parameters and the expression must use parentheses even
 * if no parameters are needed.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public class MetaValueHolderRule extends MetaRule {

    static final class LiteralValueMetadata extends Metadata {

        private final String value;

        LiteralValueMetadata(String value) {
            this.value = value;
        }

        @Override
        public void applyMetadata(FaceletContext ctx, Object instance) {
            ((ValueHolder) instance).setValue(value);
        }
    }

    static final class MetaValueExpressionMetadata extends Metadata {

        private final TagAttribute attr;

        MetaValueExpressionMetadata(TagAttribute attr) {
            this.attr = attr;
        }

        @Override
        public void applyMetadata(FaceletContext ctx, Object instance) {
            ValueExpression originalExpression = attr.getValueExpression(ctx,
                    Object.class);
            ((UIComponent) instance).setValueExpression("value",
                    new MetaValueExpression(originalExpression));
        }
    }

    public static final MetaValueHolderRule Instance = new MetaValueHolderRule();

    @Override
    public Metadata applyRule(String name, TagAttribute attribute,
            MetadataTarget meta) {
        if (meta.isTargetInstanceOf(ValueHolder.class)) {
            if ("value".equals(name)) {
                if (attribute.isLiteral()) {
                    return new LiteralValueMetadata(attribute.getValue());
                } else {
                    return new MetaValueExpressionMetadata(attribute);
                }
            }

        }
        return null;
    }

}
