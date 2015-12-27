/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.platform.ui.web.application;

import javax.faces.application.ResourceHandler;
import javax.faces.application.ResourceHandlerWrapper;
import javax.faces.application.ViewResource;
import javax.faces.context.FacesContext;

import org.nuxeo.ecm.web.resources.api.service.WebResourceManager;

/**
 * Custom resource handler resolving resources thanks to {@link WebResourceManager} service, and handling error
 * management of unknown resources.
 *
 * @since 6.0
 */
public class NuxeoResourceHandler extends ResourceHandlerWrapper {

    protected ResourceHandler wrapped;

    public NuxeoResourceHandler(ResourceHandler wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public ViewResource createViewResource(FacesContext facesContext, String resourceName) {
        if (resourceName.startsWith(NuxeoUnknownResource.MARKER)) {
            return new NuxeoUnknownResource(resourceName.substring(NuxeoUnknownResource.MARKER.length()));
        }
        ViewResource res = wrapped.createViewResource(facesContext, resourceName);
        if (res == null) {
            res = new NuxeoUnknownResource(resourceName);
        }
        return res;
    }

    @Override
    public ResourceHandler getWrapped() {
        return wrapped;
    }

}
