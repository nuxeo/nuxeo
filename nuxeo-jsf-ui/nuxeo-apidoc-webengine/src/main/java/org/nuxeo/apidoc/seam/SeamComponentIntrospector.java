/*
 * (C) Copyright 2006-2010 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thierry Delprat
 */
package org.nuxeo.apidoc.seam;

import java.util.List;

import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Startup;
import org.nuxeo.apidoc.api.SeamComponentInfo;

@Startup
@Name("nuxeoSeamIntrospector")
@Scope(ScopeType.APPLICATION)
public class SeamComponentIntrospector {

    protected HttpServletRequest getRequest() {
        FacesContext fContext = FacesContext.getCurrentInstance();
        if (fContext == null) {
            return null;
        }
        return (HttpServletRequest) fContext.getExternalContext().getRequest();
    }

    @Factory(value = "nuxeoSeamComponents", scope = ScopeType.EVENT)
    public List<SeamComponentInfo> getNuxeoComponents() {
        return SeamRuntimeIntrospector.listNuxeoComponents();
    }
}
