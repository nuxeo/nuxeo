/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.styleguide;

import static org.jboss.seam.ScopeType.APPLICATION;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;

import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.styleguide.service.StyleGuideService;
import org.nuxeo.ecm.styleguide.service.descriptors.IconDescriptor;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 5.7
 */
@Name("styleGuideIconActions")
@Scope(APPLICATION)
public class StyleGuideIconActions {

    @Factory("styleGuideIcons")
    public Map<String, List<IconDescriptor>> getIcons()
            throws MalformedURLException, IOException, URISyntaxException,
            Exception {
        FacesContext ctx = FacesContext.getCurrentInstance();
        StyleGuideService service = Framework.getService(StyleGuideService.class);
        Map<String, List<IconDescriptor>> res = service.getIconsByCat(
                ctx.getExternalContext(), "/icons");
        return res;
    }

}