/*
 * (C) Copyright 2006-2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 * $Id: TagConfigFactory.java 28460 2008-01-03 15:34:05Z sfermigier $
 */

package org.nuxeo.ecm.platform.ui.web.tag.handler;

import javax.faces.view.facelets.ComponentConfig;
import javax.faces.view.facelets.ConverterConfig;
import javax.faces.view.facelets.FaceletHandler;
import javax.faces.view.facelets.Tag;
import javax.faces.view.facelets.TagAttribute;
import javax.faces.view.facelets.TagAttributes;
import javax.faces.view.facelets.TagConfig;
import javax.faces.view.facelets.ValidatorConfig;

import org.nuxeo.ecm.platform.ui.web.binding.alias.UIAliasHolder;

import com.sun.faces.facelets.tag.TagAttributeImpl;
import com.sun.faces.facelets.tag.TagAttributesImpl;

/**
 * Helper for generating configs outside of a library context.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public final class TagConfigFactory {

    private TagConfigFactory() {
    }

    private static class TagConfigWrapper implements TagConfig {

        protected final Tag tag;

        protected final String tagId;

        protected final FaceletHandler nextHandler;

        TagConfigWrapper(TagConfig tagConfig, String tagConfigId, TagAttributes attributes, FaceletHandler nextHandler) {
            tag = new Tag(tagConfig.getTag(), getOrCreateTagAttributes(attributes));
            if (tagConfigId == null) {
                tagId = tagConfig.getTagId();
            } else {
                tagId = tagConfig.getTagId() + tagConfigId;
            }
            this.nextHandler = nextHandler;
        }

        @Override
        public FaceletHandler getNextHandler() {
            return nextHandler;
        }

        @Override
        public Tag getTag() {
            return tag;
        }

        @Override
        public String getTagId() {
            return tagId;
        }
    }

    private static class ComponentConfigWrapper extends TagConfigWrapper implements ComponentConfig {

        protected final String componentType;

        protected final String rendererType;

        ComponentConfigWrapper(TagConfig tagConfig, String tagConfigId, TagAttributes attributes,
                FaceletHandler nextHandler, String componentType, String rendererType) {
            super(tagConfig, tagConfigId, getOrCreateTagAttributes(attributes), nextHandler);
            this.componentType = componentType;
            this.rendererType = rendererType;
        }

        @Override
        public String getComponentType() {
            return componentType;
        }

        @Override
        public String getRendererType() {
            return rendererType;
        }
    }

    private static class ConverterConfigWrapper extends TagConfigWrapper implements ConverterConfig {

        protected final String converterId;

        ConverterConfigWrapper(TagConfig tagConfig, String tagConfigId, TagAttributes attributes,
                FaceletHandler nextHandler, String converterId) {
            super(tagConfig, tagConfigId, getOrCreateTagAttributes(attributes), nextHandler);
            this.converterId = converterId;
        }

        @Override
        public String getConverterId() {
            return converterId;
        }
    }

    private static class ValidatorConfigWrapper extends TagConfigWrapper implements ValidatorConfig {

        protected final String validatorId;

        ValidatorConfigWrapper(TagConfig tagConfig, String tagConfigId, TagAttributes attributes,
                FaceletHandler nextHandler, String validatorId) {
            super(tagConfig, tagConfigId, getOrCreateTagAttributes(attributes), nextHandler);
            this.validatorId = validatorId;
        }

        @Override
        public String getValidatorId() {
            return validatorId;
        }
    }

    public static TagConfig createTagConfig(TagConfig tagConfig, String tagConfigId, TagAttributes attributes,
            FaceletHandler nextHandler) {
        return new TagConfigWrapper(tagConfig, tagConfigId, attributes, nextHandler);
    }

    public static ComponentConfig createComponentConfig(TagConfig tagConfig, String tagConfigId,
            TagAttributes attributes, FaceletHandler nextHandler, String componentType, String rendererType) {
        return new ComponentConfigWrapper(tagConfig, tagConfigId, attributes, nextHandler, componentType, rendererType);
    }

    public static ConverterConfig createConverterConfig(TagConfig tagConfig, String tagConfigId,
            TagAttributes attributes, FaceletHandler nextHandler, String converterId) {
        return new ConverterConfigWrapper(tagConfig, tagConfigId, attributes, nextHandler, converterId);
    }

    public static ValidatorConfig createValidatorConfig(TagConfig tagConfig, String tagConfigId,
            TagAttributes attributes, FaceletHandler nextHandler, String validatorId) {
        return new ValidatorConfigWrapper(tagConfig, tagConfigId, attributes, nextHandler, validatorId);
    }

    /**
     * @since 6.0
     */
    public static ComponentConfig createAliasTagConfig(TagConfig tagConfig, String tagConfigId,
            TagAttributes attributes, FaceletHandler nextHandler) {
        return new ComponentConfigWrapper(tagConfig, tagConfigId, attributes, nextHandler,
                UIAliasHolder.COMPONENT_TYPE, null);
    }

    /**
     * @since 6.0
     */
    public static ComponentConfig createAliasTagConfig(TagConfig tagConfig, String tagConfigId, String var,
            String value, String cache, String anchor, FaceletHandler nextHandler) {
        TagAttribute[] attrs = new TagAttribute[4];
        attrs[0] = createAttribute(tagConfig, "var", var);
        attrs[1] = createAttribute(tagConfig, "value", value);
        attrs[2] = createAttribute(tagConfig, "cache", cache);
        attrs[3] = createAttribute(tagConfig, "anchor", anchor);
        TagAttributes attributes = new TagAttributesImpl(attrs);
        return new ComponentConfigWrapper(tagConfig, tagConfigId, attributes, nextHandler,
                UIAliasHolder.COMPONENT_TYPE, null);
    }

    protected static TagAttribute createAttribute(TagConfig tagConfig, String name, String value) {
        return new TagAttributeImpl(tagConfig.getTag().getLocation(), "", name, name, value);
    }

    protected static TagAttributes getOrCreateTagAttributes(TagAttributes attributes) {
        if (attributes == null) {
            return new TagAttributesImpl(new TagAttribute[0]);
        }
        return attributes;
    }

}
