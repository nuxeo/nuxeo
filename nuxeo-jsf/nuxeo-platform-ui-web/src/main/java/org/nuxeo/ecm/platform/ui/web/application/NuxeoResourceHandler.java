/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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
package org.nuxeo.ecm.platform.ui.web.application;

import javax.faces.application.ResourceHandler;
import javax.faces.application.ResourceHandlerWrapper;
import javax.faces.application.ViewResource;
import javax.faces.context.FacesContext;

/**
 * Custom resource handler for error management of unknown resources.
 *
 * @since 6.0
 */
public class NuxeoResourceHandler extends ResourceHandlerWrapper {

    protected ResourceHandler wrapped;

    public NuxeoResourceHandler(ResourceHandler wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public ViewResource createViewResource(FacesContext facesContext,
            String resourceName) {
        if (resourceName.startsWith(NuxeoUnknownResource.MARKER)) {
            return new NuxeoUnknownResource(
                    resourceName.substring(NuxeoUnknownResource.MARKER.length()));
        }
        ViewResource res = wrapped.createViewResource(facesContext,
                resourceName);
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