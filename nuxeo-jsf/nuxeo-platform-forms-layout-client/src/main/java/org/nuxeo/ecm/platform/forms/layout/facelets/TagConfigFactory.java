/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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

package org.nuxeo.ecm.platform.forms.layout.facelets;

import javax.faces.view.facelets.ComponentConfig;
import javax.faces.view.facelets.ConverterConfig;
import javax.faces.view.facelets.FaceletHandler;
import javax.faces.view.facelets.TagAttributes;
import javax.faces.view.facelets.TagConfig;
import javax.faces.view.facelets.ValidatorConfig;

/**
 * Helper for generating configs outside of a library context.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 * @deprecated since 5.7: use {@link org.nuxeo.ecm.platform.ui.web.tag.handler.TagConfigFactory} instead
 */
@Deprecated
public final class TagConfigFactory {

    /**
     * @deprecated since 5.7: use {@link org.nuxeo.ecm.platform.ui.web.tag.handler.TagConfigFactory} instead
     **/
    @Deprecated
    public static TagConfig createTagConfig(TagConfig tagConfig, String tagConfigId, TagAttributes attributes,
            FaceletHandler nextHandler) {
        return org.nuxeo.ecm.platform.ui.web.tag.handler.TagConfigFactory.createTagConfig(tagConfig, tagConfigId,
                attributes, nextHandler);
    }

    /**
     * @deprecated since 5.7: use {@link org.nuxeo.ecm.platform.ui.web.tag.handler.TagConfigFactory} instead
     **/
    @Deprecated
    public static ComponentConfig createComponentConfig(TagConfig tagConfig, String tagConfigId,
            TagAttributes attributes, FaceletHandler nextHandler, String componentType, String rendererType) {
        return org.nuxeo.ecm.platform.ui.web.tag.handler.TagConfigFactory.createComponentConfig(tagConfig, tagConfigId,
                attributes, nextHandler, componentType, rendererType);
    }

    /**
     * @deprecated since 5.7: use {@link org.nuxeo.ecm.platform.ui.web.tag.handler.TagConfigFactory} instead
     **/
    @Deprecated
    public static ConverterConfig createConverterConfig(TagConfig tagConfig, String tagConfigId,
            TagAttributes attributes, FaceletHandler nextHandler, String converterId) {
        return org.nuxeo.ecm.platform.ui.web.tag.handler.TagConfigFactory.createConverterConfig(tagConfig, tagConfigId,
                attributes, nextHandler, converterId);
    }

    /**
     * @deprecated since 5.7: use {@link org.nuxeo.ecm.platform.ui.web.tag.handler.TagConfigFactory} instead
     **/
    @Deprecated
    public static ValidatorConfig createValidatorConfig(TagConfig tagConfig, String tagConfigId,
            TagAttributes attributes, FaceletHandler nextHandler, String validatorId) {
        return org.nuxeo.ecm.platform.ui.web.tag.handler.TagConfigFactory.createValidatorConfig(tagConfig, tagConfigId,
                attributes, nextHandler, validatorId);
    }

}
