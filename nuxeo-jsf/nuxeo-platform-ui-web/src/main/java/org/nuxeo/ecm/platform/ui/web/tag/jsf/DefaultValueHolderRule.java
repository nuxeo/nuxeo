/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
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
package org.nuxeo.ecm.platform.ui.web.tag.jsf;

import javax.el.ValueExpression;
import javax.faces.component.UIComponent;
import javax.faces.component.ValueHolder;
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.MetaRule;
import javax.faces.view.facelets.Metadata;
import javax.faces.view.facelets.MetadataTarget;
import javax.faces.view.facelets.TagAttribute;

import org.nuxeo.ecm.platform.ui.web.binding.DefaultValueExpression;

/**
 * Value holder rule, handling a default value using a
 * {@link DefaultValueExpression} when a "defaultValue" attribute is set on the
 * tag.
 * <p>
 * Assumes the standard value holder rule has already been processed, so that
 * the corresponding value expression is already available on the component.
 *
 * @since 5.7.3
 */
public class DefaultValueHolderRule extends MetaRule {

    public static final DefaultValueHolderRule Instance = new DefaultValueHolderRule();

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

    static final class DynamicDefaultValueExpressionMetadata extends Metadata {

        private final TagAttribute attr;

        DynamicDefaultValueExpressionMetadata(TagAttribute attr) {
            this.attr = attr;
        }

        @Override
        public void applyMetadata(FaceletContext ctx, Object instance) {
            UIComponent comp = (UIComponent) instance;
            ValueExpression defaultVe = attr.getValueExpression(ctx,
                    Object.class);
            ValueExpression ve = comp.getValueExpression("value");
            comp.setValueExpression("value", new DefaultValueExpression(ve,
                    defaultVe));
        }
    }

    @Override
    public Metadata applyRule(String name, TagAttribute attribute,
            MetadataTarget meta) {
        if (meta.isTargetInstanceOf(ValueHolder.class)) {

            if ("defaultValue".equals(name)) {
                if (attribute.isLiteral()) {
                    return new LiteralValueMetadata(attribute.getValue());
                } else {
                    return new DynamicDefaultValueExpressionMetadata(attribute);
                }
            }

        }
        return null;
    }

}
