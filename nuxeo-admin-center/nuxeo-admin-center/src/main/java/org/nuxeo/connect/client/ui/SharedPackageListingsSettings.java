/*
 * (C) Copyright 2011 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 */
package org.nuxeo.connect.client.ui;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.nuxeo.ecm.webengine.jaxrs.context.RequestContext;

/**
 * Provide contextual access to the {@link ListingFilterSetting} for each listing.
 * Use HttpSession to store a map of {@link ListingFilterSetting}
 *
 * This class is used to share state between the WebEngine and the JSF parts
 * @author Tiry (tdelprat@nuxeo.com)
 *
 */
public class SharedPackageListingsSettings implements Serializable {

    private static final long serialVersionUID = 1L;

    protected Map<String, ListingFilterSetting> settings = new HashMap<String, ListingFilterSetting>();

    public static final String SESSION_KEY = "org.nuxeo.connect.client.ui.PackageListingSettings";

    public ListingFilterSetting get(String listName) {
        if (!settings.containsKey(listName)) {
            settings.put(listName, new ListingFilterSetting());
        }
        return settings.get(listName);
    }

    public static SharedPackageListingsSettings instance() {

        HttpServletRequest request = null;
        if (FacesContext.getCurrentInstance() != null) {
            request = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
        } else if (RequestContext.getActiveContext() != null) {
            request = RequestContext.getActiveContext().getRequest();
        }

        if (request != null) {
            return instance(request);
        }
        return null;
    }

    public static SharedPackageListingsSettings instance(HttpServletRequest request) {
        return instance(request.getSession(true));
    }

    public static SharedPackageListingsSettings instance(HttpSession session) {
        Object val = session.getAttribute(SESSION_KEY);
        if (val == null || !(val instanceof SharedPackageListingsSettings)) {
            val = new SharedPackageListingsSettings();
            session.setAttribute(SESSION_KEY, val);
        }
        return (SharedPackageListingsSettings) val;
    }

}
