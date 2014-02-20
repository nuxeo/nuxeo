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
 * @deprecated since 5.7: use
 *             {@link org.nuxeo.ecm.platform.ui.web.tag.handler.TagConfigFactory}
 *             instead
 */
@Deprecated
public final class TagConfigFactory {

    /**
     * @deprecated since 5.7: use
     *             {@link org.nuxeo.ecm.platform.ui.web.tag.handler.TagConfigFactory}
     *             instead
     **/
    @Deprecated
    public static TagConfig createTagConfig(TagConfig tagConfig,
            String tagConfigId, TagAttributes attributes,
            FaceletHandler nextHandler) {
        return org.nuxeo.ecm.platform.ui.web.tag.handler.TagConfigFactory.createTagConfig(
                tagConfig, tagConfigId, attributes, nextHandler);
    }

    /**
     * @deprecated since 5.7: use
     *             {@link org.nuxeo.ecm.platform.ui.web.tag.handler.TagConfigFactory}
     *             instead
     **/
    @Deprecated
    public static ComponentConfig createComponentConfig(TagConfig tagConfig,
            String tagConfigId, TagAttributes attributes,
            FaceletHandler nextHandler, String componentType,
            String rendererType) {
        return org.nuxeo.ecm.platform.ui.web.tag.handler.TagConfigFactory.createComponentConfig(
                tagConfig, tagConfigId, attributes, nextHandler, componentType,
                rendererType);
    }

    /**
     * @deprecated since 5.7: use
     *             {@link org.nuxeo.ecm.platform.ui.web.tag.handler.TagConfigFactory}
     *             instead
     **/
    @Deprecated
    public static ConverterConfig createConverterConfig(TagConfig tagConfig,
            String tagConfigId, TagAttributes attributes,
            FaceletHandler nextHandler, String converterId) {
        return org.nuxeo.ecm.platform.ui.web.tag.handler.TagConfigFactory.createConverterConfig(
                tagConfig, tagConfigId, attributes, nextHandler, converterId);
    }

    /**
     * @deprecated since 5.7: use
     *             {@link org.nuxeo.ecm.platform.ui.web.tag.handler.TagConfigFactory}
     *             instead
     **/
    @Deprecated
    public static ValidatorConfig createValidatorConfig(TagConfig tagConfig,
            String tagConfigId, TagAttributes attributes,
            FaceletHandler nextHandler, String validatorId) {
        return org.nuxeo.ecm.platform.ui.web.tag.handler.TagConfigFactory.createValidatorConfig(
                tagConfig, tagConfigId, attributes, nextHandler, validatorId);
    }

}