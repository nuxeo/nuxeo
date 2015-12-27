/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.gwt.client.ui.editor;

import org.nuxeo.ecm.platform.gwt.client.SmartClient;
import org.nuxeo.ecm.platform.gwt.client.ui.Container;
import org.nuxeo.ecm.platform.gwt.client.ui.Site;
import org.nuxeo.ecm.platform.gwt.client.ui.SiteEventHandler;

import com.smartgwt.client.types.Side;
import com.smartgwt.client.widgets.tab.Tab;
import com.smartgwt.client.widgets.tab.TabSet;
import com.smartgwt.client.widgets.tab.events.CloseClickHandler;
import com.smartgwt.client.widgets.tab.events.TabCloseClickEvent;
import com.smartgwt.client.widgets.tab.events.TabSelectedEvent;
import com.smartgwt.client.widgets.tab.events.TabSelectedHandler;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class TabsContainer implements Container {

    protected SiteEventHandler eventHandler;
    protected TabSet tabs;

    public TabsContainer() {
        tabs = createTabs();
        tabs.addCloseClickHandler(new CloseClickHandler() {
            public void onCloseClick(TabCloseClickEvent event) {
                String id = SmartClient.getTabId(event);
                if (eventHandler != null) {
                    eventHandler.handleSiteEvent(id, SiteEventHandler.SITE_CLOSED);
                }
                tabs.removeTab(id);
            };
        });
        tabs.addTabSelectedHandler(new TabSelectedHandler() {
            public void onTabSelected(TabSelectedEvent event) {
                if (eventHandler != null) {
                    eventHandler.handleSiteEvent(event.getID(), SiteEventHandler.SITE_ACTIVATED);
                }
            }
        });
    }

    protected TabSet createTabs() {
        TabSet tabs = new TabSet();
        tabs.setTabBarPosition(Side.BOTTOM);
        tabs.setHeight100();
        return tabs;
    }

    public TabSet getWidget() {
        return tabs;
    }

    public String getHandle(Site site) {
        return (String)site.getHandle();
    }

    public Tab getTab(Site site) {
        return tabs.getTab(getHandle(site));
    }

    public void activateSite(Site site) {
        tabs.selectTab(getHandle(site));
    }


    public void deactivateSite(Site site) {
        // do nothing - operation not supported
    }

    public Object getActiveSiteHandle() {
        return tabs.getTab(tabs.getSelectedTab()).getID();
    }

    public void disableSite(Site site) {
        Tab tab = getTab(site);
        if (tab != null) {
            tabs.updateTab(tab, null); // avoid destroying tab panel
            tabs.removeTab(tab);
        }
    }

    public void enableSite(Site site) {
        Tab tab = getTab(site);
        if (tab != null) {
            tabs.addTab(tab);
        }
    }

    public boolean isSiteActive(Site site) {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean isSiteEnabled(Site site) {
        // TODO Auto-generated method stub
        return false;
    }

    public void closeSite(Site site) {
        tabs.removeTab(getHandle(site));
    }

    public void updateSiteIcon(Site site) {
        String id = getHandle(site);
        String icon = site.getIcon();
        if (icon != null) {
            SmartClient.setTabIcon(tabs, id, icon);
        }
    }

    public void updateSiteTitle(Site site) {
        String id = getHandle(site);
        String title = site.getTitle();
        if (title != null) {
            tabs.setTabTitle(id, site.getTitle());
        }
    }

    public Object createHandle(Site site) {
        Tab tab = new Tab();
        tabs.addTab(tab);
        return tab.getID();
    }

    public void installWidget(Site site) {
        Tab tab = getTab(site);
        tabs.updateTab(tab, SmartClient.toCanvas(site.getView().getWidget()));
    }

    public void closeAll() {
        Tab[] ar = tabs.getTabs();
        for (Tab tab : ar) {
            tabs.removeTab(tab);
        }
    }

    public void clear() {
        Tab[] ar = tabs.getTabs();
        for (Tab tab : ar) {
            tabs.updateTab(tab, null); // avoid destroying tab panel - to be able to reuse it
            tabs.removeTab(tab);
        }
    }

    public void setSiteEventHandler(SiteEventHandler handler) {
        this.eventHandler = handler;
    }

    public SiteEventHandler getSiteEventHandler() {
        return eventHandler;
    }


}
