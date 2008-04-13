/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.site.template;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
@XObject("template")
public class SitePageTemplate {

    @XNode("@name")
    protected String name;

    @XNode("@extends")
    protected String zuper = "default";

    @XNodeMap(value="view", key="name", componentType=SiteObjectView.class, type=ConcurrentHashMap.class)
    protected ConcurrentMap<String, SiteObjectView> views;


    SitePageTemplate() {}

    public SitePageTemplate(String name) {
        this (name, null);
    }

    public SitePageTemplate(String name, String zuper) {
        this.name = name;
        this.zuper = zuper;
        views = new ConcurrentHashMap<String, SiteObjectView>();
    }

    public String getName() {
        return name;
    }

    public SitePageTemplate getSuper() {
        if (zuper == null) {
            return null;
        }
        SitePageTemplate obj = getSiteManager().getTemplate(zuper);
        if (obj == null) {
            return getSiteManager().getDefaultSiteObject();
        }
        return obj;
    }


    public SiteObjectView[] getActions() {
        return views.values().toArray(new SiteObjectView[views.size()]);
    }

    public SiteObjectView getView(String name) {
        SiteObjectView view = views.get(name);
        if (view == null) {
            SitePageTemplate base = getSuper();
            if (base != null) {
                view = base.getView(name);
                if (view != null) {
                    views.put(name, view);
                }
            }
        }
        return view;
    }

    public Map<String,SiteObjectView> getViewMap() {
        return views;
    }

    public void addView(SiteObjectView view) {
        views.put(view.name, view);
    }

    public SiteManager getSiteManager() {
        return Framework.getLocalService(SiteManager.class);
    }

}
