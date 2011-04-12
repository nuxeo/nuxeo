/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 * $Id: TagConfigFactory.java 28460 2008-01-03 15:34:05Z sfermigier $
 */

package org.nuxeo.ecm.platform.forms.layout.facelets;

import com.sun.facelets.FaceletHandler;
import com.sun.facelets.tag.Tag;
import com.sun.facelets.tag.TagAttributes;
import com.sun.facelets.tag.TagConfig;
import com.sun.facelets.tag.jsf.ComponentConfig;
import com.sun.facelets.tag.jsf.ConverterConfig;
import com.sun.facelets.tag.jsf.ValidatorConfig;

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

        TagConfigWrapper(TagConfig tagConfig, String tagConfigId,
                TagAttributes attributes, FaceletHandler nextHandler) {
            tag = new Tag(tagConfig.getTag(), attributes);
            if (tagConfigId == null) {
                tagId = tagConfig.getTagId();
            } else {
                tagId = tagConfig.getTagId() + tagConfigId;
            }
            this.nextHandler = nextHandler;
        }

        public FaceletHandler getNextHandler() {
            return nextHandler;
        }

        public Tag getTag() {
            return tag;
        }

        public String getTagId() {
            return tagId;
        }
    }

    private static class ComponentConfigWrapper extends TagConfigWrapper
            implements ComponentConfig {

        protected final String componentType;

        protected final String rendererType;

        ComponentConfigWrapper(TagConfig tagConfig, String tagConfigId,
                TagAttributes attributes, FaceletHandler nextHandler,
                String componentType, String rendererType) {
            super(tagConfig, tagConfigId, attributes, nextHandler);
            this.componentType = componentType;
            this.rendererType = rendererType;
        }

        public String getComponentType() {
            return componentType;
        }

        public String getRendererType() {
            return rendererType;
        }
    }

    private static class ConverterConfigWrapper extends TagConfigWrapper
            implements ConverterConfig {

        protected final String converterId;

        ConverterConfigWrapper(TagConfig tagConfig, String tagConfigId,
                TagAttributes attributes, FaceletHandler nextHandler,
                String converterId) {
            super(tagConfig, tagConfigId, attributes, nextHandler);
            this.converterId = converterId;
        }

        public String getConverterId() {
            return converterId;
        }
    }

    private static class ValidatorConfigWrapper extends TagConfigWrapper
            implements ValidatorConfig {

        protected final String validatorId;

        ValidatorConfigWrapper(TagConfig tagConfig, String tagConfigId,
                TagAttributes attributes, FaceletHandler nextHandler,
                String validatorId) {
            super(tagConfig, tagConfigId, attributes, nextHandler);
            this.validatorId = validatorId;
        }

        public String getValidatorId() {
            return validatorId;
        }
    }

    /**
     * @deprecated since 5.4.2, use
     *             {@link TagConfigFactory#createTagConfig(TagConfig, String, TagAttributes, FaceletHandler)}
     *             instead.
     */
    @Deprecated
    public static TagConfig createTagConfig(TagConfig tagConfig,
            TagAttributes attributes, FaceletHandler nextHandler) {
        return createTagConfig(tagConfig, null, attributes, nextHandler);
    }

    public static TagConfig createTagConfig(TagConfig tagConfig,
            String tagConfigId, TagAttributes attributes,
            FaceletHandler nextHandler) {
        return new TagConfigWrapper(tagConfig, tagConfigId, attributes,
                nextHandler);
    }

    /**
     * @deprecated since 5.4.2, use
     *             {@link TagConfigFactory#createComponentConfig(TagConfig, String, TagAttributes, FaceletHandler, String, String)}
     *             instead.
     */
    @Deprecated
    public static ComponentConfig createComponentConfig(TagConfig tagConfig,
            TagAttributes attributes, FaceletHandler nextHandler,
            String componentType, String rendererType) {
        return createComponentConfig(tagConfig, null, attributes, nextHandler,
                componentType, rendererType);
    }

    public static ComponentConfig createComponentConfig(TagConfig tagConfig,
            String tagConfigId, TagAttributes attributes,
            FaceletHandler nextHandler, String componentType,
            String rendererType) {
        return new ComponentConfigWrapper(tagConfig, tagConfigId, attributes,
                nextHandler, componentType, rendererType);
    }

    /**
     * @deprecated since 5.4.2, use
     *             {@link TagConfigFactory#createConverterConfig(TagConfig, String, TagAttributes, FaceletHandler, String)}
     *             instead.
     */
    @Deprecated
    public static ConverterConfig createConverterConfig(TagConfig tagConfig,
            TagAttributes attributes, FaceletHandler nextHandler,
            String converterId) {
        return createConverterConfig(tagConfig, null, attributes, nextHandler,
                converterId);
    }

    public static ConverterConfig createConverterConfig(TagConfig tagConfig,
            String tagConfigId, TagAttributes attributes,
            FaceletHandler nextHandler, String converterId) {
        return new ConverterConfigWrapper(tagConfig, tagConfigId, attributes,
                nextHandler, converterId);
    }

    /**
     * @deprecated since 5.4.2, use
     *             {@link TagConfigFactory#createValidatorConfig(TagConfig, String, TagAttributes, FaceletHandler, String)}
     *             instead.
     */
    @Deprecated
    public static ValidatorConfig createValidatorConfig(TagConfig tagConfig,
            TagAttributes attributes, FaceletHandler nextHandler,
            String validatorId) {
        return createValidatorConfig(tagConfig, null, attributes, nextHandler,
                validatorId);
    }

    public static ValidatorConfig createValidatorConfig(TagConfig tagConfig,
            String tagConfigId, TagAttributes attributes,
            FaceletHandler nextHandler, String validatorId) {
        return new ValidatorConfigWrapper(tagConfig, tagConfigId, attributes,
                nextHandler, validatorId);
    }

}
