/*
 * (C) Copyright 2006-2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
