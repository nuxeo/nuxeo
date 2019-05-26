/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo
 */
package org.nuxeo.ecm.admin;

import org.nuxeo.connect.client.ui.SharedPackageListingsSettings;
import org.nuxeo.connect.client.ui.SharedPackageListingsSettings.RequestResolver;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.DefaultComponent;

import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;

public class AdminJSFComponent extends DefaultComponent {

    static RequestResolver resolver = new RequestResolver() {
        @Override
        public boolean isActive() {
            return FacesContext.getCurrentInstance() != null;
        }

        @Override
        public HttpServletRequest resolve() {
            return (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
        }

        @Override
        public String getId() {
            return "jsf";
        }
    };

    @Override
    public void activate(ComponentContext context) {
        super.activate(context);
        SharedPackageListingsSettings.addRequestResolver(resolver);
    }

    @Override
    public void deactivate(ComponentContext context) {
        super.deactivate(context);
        SharedPackageListingsSettings.removeRequestResolver(resolver.getId());
    }
}
