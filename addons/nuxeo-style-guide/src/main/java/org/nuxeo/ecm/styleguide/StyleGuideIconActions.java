/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
    public Map<String, List<IconDescriptor>> getIcons() throws MalformedURLException, IOException, URISyntaxException,
            Exception {
        FacesContext ctx = FacesContext.getCurrentInstance();
        StyleGuideService service = Framework.getService(StyleGuideService.class);
        Map<String, List<IconDescriptor>> res = service.getIconsByCat(ctx.getExternalContext(), "/icons");
        return res;
    }

}
