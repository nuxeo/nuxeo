/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.web.resources.jsf.negotiators;

import javax.faces.context.FacesContext;

import org.nuxeo.runtime.api.Framework;
import org.nuxeo.theme.styling.negotiation.AbstractNegotiator;
import org.nuxeo.theme.styling.service.ThemeStylingService;
import org.nuxeo.theme.styling.service.descriptors.FlavorDescriptor;

/**
 * Negotiator that returns the default flavor configured for negotiated theme page.
 *
 * @see ThemeStylingService
 * @see FlavorDescriptor
 * @since 5.5
 */
public class DefaultPageFlavor extends AbstractNegotiator {

    @Override
    public String getResult(String target, Object context) {
        FacesContext faces = null;
        if (context instanceof FacesContext) {
            faces = (FacesContext) context;
        } else {
            return null;
        }
        String theme = (String) faces.getExternalContext().getRequestMap().get(getProperty("negotiatedPageVariable"));
        if (theme != null) {
            ThemeStylingService service = Framework.getService(ThemeStylingService.class);
            return service.getDefaultFlavorName(theme);
        }
        return null;
    }
}
