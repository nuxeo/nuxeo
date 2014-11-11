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
 * $Id: GenericValueHolderRule.java 28456 2008-01-03 12:01:11Z sfermigier $
 */

package org.nuxeo.ecm.platform.ui.web.tag.jsf;

import javax.el.MethodExpression;
import javax.el.ValueExpression;
import javax.faces.component.UIComponent;
import javax.faces.component.ValueHolder;
import javax.faces.convert.Converter;
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.MetaRule;
import javax.faces.view.facelets.Metadata;
import javax.faces.view.facelets.MetadataTarget;
import javax.faces.view.facelets.TagAttribute;

import org.nuxeo.ecm.platform.ui.web.binding.MethodValueExpression;
import org.nuxeo.ecm.platform.ui.web.util.ComponentTagUtils;

import com.sun.faces.facelets.el.LegacyValueBinding;

/**
 * Generic value rule, used to evaluate an expression as a regular value
 * expression, or invoking a method expression.
 * <p>
 * The method can have parameters and the expression must use parentheses even
 * if no parameters are needed.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 * @deprecated since 5.7.3: resolving method expressions for value expressions
 *             is now supported by the default EL, this is now useless
 */
@Deprecated
public class GenericValueHolderRule extends MetaRule {

    static final class LiteralConverterMetadata extends Metadata {

        private final String converterId;

        LiteralConverterMetadata(String converterId) {
            this.converterId = converterId;
        }

        @Override
        public void applyMetadata(FaceletContext ctx, Object instance) {
            ((ValueHolder) instance).setConverter(ctx.getFacesContext().getApplication().createConverter(
                    converterId));
        }
    }

    static final class DynamicConverterMetadata2 extends Metadata {

        private final TagAttribute attr;

        DynamicConverterMetadata2(TagAttribute attr) {
            this.attr = attr;
        }

        @Override
        public void applyMetadata(FaceletContext ctx, Object instance) {
            ((UIComponent) instance).setValueExpression("converter",
                    attr.getValueExpression(ctx, Converter.class));
        }
    }

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

    static final class DynamicValueExpressionMetadata extends Metadata {

        private final TagAttribute attr;

        DynamicValueExpressionMetadata(TagAttribute attr) {
            this.attr = attr;
        }

        @Override
        public void applyMetadata(FaceletContext ctx, Object instance) {
            ((UIComponent) instance).setValueExpression("value",
                    attr.getValueExpression(ctx, Object.class));
        }
    }

    static final class MethodValueBindingMetadata extends Metadata {

        private final TagAttribute attr;

        MethodValueBindingMetadata(TagAttribute attr) {
            this.attr = attr;
        }

        @Override
        public void applyMetadata(FaceletContext ctx, Object instance) {
            Class[] paramTypesClasses = new Class[0];
            Class returnType = Object.class;
            MethodExpression meth = attr.getMethodExpression(ctx, returnType,
                    paramTypesClasses);
            ValueExpression ve = new MethodValueExpression(
                    ctx.getFunctionMapper(), ctx.getVariableMapper(), meth,
                    paramTypesClasses);
            ((UIComponent) instance).setValueBinding("value",
                    new LegacyValueBinding(ve));
        }
    }

    public static final GenericValueHolderRule Instance = new GenericValueHolderRule();

    @Override
    public Metadata applyRule(String name, TagAttribute attribute,
            MetadataTarget meta) {
        if (meta.isTargetInstanceOf(ValueHolder.class)) {

            if ("converter".equals(name)) {
                if (attribute.isLiteral()) {
                    return new LiteralConverterMetadata(attribute.getValue());
                } else {
                    return new DynamicConverterMetadata2(attribute);
                }
            }

            if ("genericValue".equals(name)) {
                if (attribute.isLiteral()) {
                    return new LiteralValueMetadata(attribute.getValue());
                } else {
                    String value = attribute.getValue();
                    if (ComponentTagUtils.isMethodReference(value)) {
                        // value expression resolved invoking a method
                        // expression
                        return new MethodValueBindingMetadata(attribute);
                    } else {
                        // regular value expression
                        return new DynamicValueExpressionMetadata(attribute);
                    }
                }
            }

        }
        return null;
    }

}
