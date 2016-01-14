/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.platform.ui.web.validator;

import java.io.IOException;

import javax.faces.component.EditableValueHolder;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.Validator;
import javax.faces.view.AttachedObjectHandler;
import javax.faces.view.facelets.ComponentHandler;
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.MetaRule;
import javax.faces.view.facelets.MetaRuleset;
import javax.faces.view.facelets.MetaTagHandler;
import javax.faces.view.facelets.Metadata;
import javax.faces.view.facelets.MetadataTarget;
import javax.faces.view.facelets.Tag;
import javax.faces.view.facelets.TagAttribute;
import javax.faces.view.facelets.TagConfig;
import javax.faces.view.facelets.TagException;

import org.nuxeo.ecm.platform.ui.web.binding.MetaMethodExpression;

import com.sun.faces.facelets.tag.MetaRulesetImpl;
import com.sun.faces.facelets.tag.jsf.CompositeComponentTagHandler;
import com.sun.faces.util.Util;

/**
 * Handler for {@link MethodValidator} component, deferring parsing and evalusation of validation method attribute, so
 * that it can be referenced as a value expression.
 *
 * @since 8.1
 */
public class MethodValidatorTagHandler extends MetaTagHandler implements AttachedObjectHandler {

    @SuppressWarnings("rawtypes")
    private final static Class[] VALIDATOR_SIG = new Class[] { FacesContext.class, UIComponent.class, Object.class };

    public MethodValidatorTagHandler(TagConfig config) {
        super(config);
    }

    @Override
    public void apply(FaceletContext ctx, UIComponent parent) throws IOException {
        if (!ComponentHandler.isNew(parent)) {
            return;
        }

        if (parent instanceof EditableValueHolder) {
            applyAttachedObject(ctx.getFacesContext(), parent);
        } else if (UIComponent.isCompositeComponent(parent)) {
            CompositeComponentTagHandler.getAttachedObjectHandlers(parent).add(this);
        } else {
            throw new TagException(getTag(), "Parent not an instance of EditableValueHolder: " + parent);
        }
    }

    public Tag getTag() {
        return this.tag;
    }

    public void applyAttachedObject(FacesContext context, UIComponent parent) {
        FaceletContext ctx = (FaceletContext) context.getAttributes().get(FaceletContext.FACELET_CONTEXT_KEY);
        Validator v = context.getApplication().createValidator(MethodValidator.VALIDATOR_ID);
        setAttributes(ctx, v);
        EditableValueHolder evh = (EditableValueHolder) parent;
        evh.addValidator(v);
    }

    @Override
    public String getFor() {
        String result = null;
        TagAttribute attr = getAttribute("for");

        if (null != attr) {
            if (attr.isLiteral()) {
                result = attr.getValue();
            } else {
                FacesContext context = FacesContext.getCurrentInstance();
                FaceletContext ctx = (FaceletContext) context.getAttributes().get(FaceletContext.FACELET_CONTEXT_KEY);
                result = (String) attr.getValueExpression(ctx, String.class).getValue(ctx);
            }
        }
        return result;
    }

    @Override
    @SuppressWarnings("rawtypes")
    protected MetaRuleset createMetaRuleset(Class type) {
        Util.notNull("type", type);
        MetaRuleset m = new MetaRulesetImpl(getTag(), type);
        return m.ignore("binding").ignore("disabled").ignore("for").addRule(MethodValueHolderRule.Instance);
    }

    static final class MethodValueHolderRule extends MetaRule {

        public final static MethodValueHolderRule Instance = new MethodValueHolderRule();

        final static class ValidatorExpressionMetadata extends Metadata {
            private final TagAttribute attr;

            public ValidatorExpressionMetadata(TagAttribute attr) {
                this.attr = attr;
            }

            public void applyMetadata(FaceletContext ctx, Object instance) {
                ((MethodValidator) instance).setMethodExpression(
                        new MetaMethodExpression(attr.getMethodExpression(ctx, null, VALIDATOR_SIG),
                                ctx.getFunctionMapper(), ctx.getVariableMapper(), null, VALIDATOR_SIG));
            }
        }

        public Metadata applyRule(String name, TagAttribute attribute, MetadataTarget meta) {
            if ("method".equals(name)) {
                return new ValidatorExpressionMetadata(attribute);
            }
            return null;
        }
    }
}