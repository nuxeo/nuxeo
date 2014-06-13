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
package org.nuxeo.theme.jsf.facelets.vendor;

import java.net.URL;

import javax.faces.context.FacesContext;
import javax.faces.view.facelets.ResourceResolver;

import org.nuxeo.theme.jsf.FacesResourceResolver;

/**
 * Hooks up {@link FacesResourceResolver} to the JSF2 mechanism.
 *
 * @since 5.9.4-JSF2
 */
@SuppressWarnings("deprecation")
public class NXFaceletsResourceResolver extends ResourceResolver {

    protected ResourceResolver parent = null;

    public NXFaceletsResourceResolver(ResourceResolver parent) {
        super();
        this.parent = parent;
    }

    @Override
    public URL resolveUrl(String path) {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        org.nuxeo.theme.ResourceResolver.setInstance(new FacesResourceResolver(
                facesContext.getExternalContext()));
        try {
            return parent.resolveUrl(path);
        } finally {
            org.nuxeo.theme.ResourceResolver.setInstance(null);
        }
    }

}
