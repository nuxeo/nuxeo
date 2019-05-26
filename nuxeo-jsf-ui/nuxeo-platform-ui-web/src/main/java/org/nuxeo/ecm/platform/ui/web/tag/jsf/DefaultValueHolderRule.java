/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
import org.nuxeo.ecm.platform.ui.web.tag.handler.GenericHtmlComponentHandler;

/**
 * Value holder rule, handling a default value using a {@link DefaultValueExpression} when a "defaultValue" attribute is
 * set on the tag.
 * <p>
 * Assumes the standard value holder rule has already been processed, so that the corresponding value expression is
 * already available on the component.
 *
 * @since 5.7.3
 * @deprecated since 7.2: since this is a mixup between several attributes, this is not handled by rules anymore, see
 *             {@link GenericHtmlComponentHandler#onComponentCreated(FaceletContext, UIComponent, UIComponent)}
 */
@Deprecated
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
            ValueExpression defaultVe = attr.getValueExpression(ctx, Object.class);
            ValueExpression ve = comp.getValueExpression("value");
            comp.setValueExpression("value", new DefaultValueExpression(ve, defaultVe));
        }
    }

    @Override
    public Metadata applyRule(String name, TagAttribute attribute, MetadataTarget meta) {
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
