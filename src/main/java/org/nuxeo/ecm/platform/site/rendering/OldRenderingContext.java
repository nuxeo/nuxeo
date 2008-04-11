/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.site.rendering;

import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.site.api.SiteAwareObject;
import org.nuxeo.ecm.platform.site.servlet.SiteRequest;

/**
 * Storage for rendering context
 *
 * @author <a href="mailto:td@nuxeo.com">Thierry Delprat</a>
 *
 */

@Deprecated
public class OldRenderingContext {

    public static final String CONTAINER = "container";
    public static final String CURRENT = "current";
    public static final String CHILD = "child";
    public static final String CHILDREN = "children";
    public static final String MODE = "mode";
    public static final String REQUEST = "request";
    public static final String CORESESSION = "documentManager";
    public static final String USER = "user";

    private SiteRequest request;

    private SiteAwareObject currentSiteObject;

    private Map<String, Object> context = new HashMap<String, Object>();

    public OldRenderingContext(SiteRequest request)
    {
        this.request=request;
    }

    public void setCurrentSiteObject(SiteAwareObject siteObject)
    {
        currentSiteObject= siteObject;
    }


    public void put(String key, Object value)
    {
        context.put(key, value);
    }

    @Override
    public String toString()
    {
        StringBuffer sb = new StringBuffer();

        sb.append(CONTAINER + ":" + safeGet(CONTAINER) + "\n");
        sb.append(CURRENT + ":" + safeGet(CURRENT) + "\n");
        sb.append(CHILD + ":" + safeGet(CHILD) + "\n");
        sb.append(CHILDREN + ":" + safeGet(CHILDREN) + "\n");
        sb.append(MODE + ":" + safeGet(MODE) + "\n");

        for (String key : context.keySet())
        {
            sb.append(key + ":" + safeGet(key) + "\n");
        }
        return sb.toString();
    }

    private String safeGet(String key)
    {
        Object value = get(key);
        if (value==null)
            value="null";
        return value.toString();
    }

    public Object get(String key)
    {
        if (CONTAINER.equals(key))
        {
            return request.getTraversalParent(currentSiteObject);
        }

        if (CURRENT.equals(key))
        {
            return currentSiteObject;
        }

        if (CHILD.equals(key))
        {
            return request.getTraversalChild(currentSiteObject);
        }

        if (CHILDREN.equals(key))
        {
            try {
                return currentSiteObject.getChildren();
            } catch (ClientException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                return null;
            }
        }

        if (MODE.equals(key))
        {
            return request.getMode();
        }

        if (CORESESSION.equals(key))
        {
            return request.getDocumentManager();
        }

        if (REQUEST.equals(key))
        {
            return request;
        }

        if (USER.equals(key))
        {
            return (NuxeoPrincipal) request.getUserPrincipal();
        }

        // look in local context
        if (context.containsKey(key))
            return context.get(key);

        // look in request parameters
        if (request.getParameterMap().containsKey(key))
        {
            return request.getParameter(key);
        }

        // look in requests attributes
        Object value = request.getAttribute(key);
        if (value!=null)
            return value;

        return null;
    }
}
