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

package org.nuxeo.ecm.platform.gwt.client.ui.impl;

import org.nuxeo.ecm.platform.gwt.client.Framework;
import org.nuxeo.ecm.platform.gwt.client.SmartClient;
import org.nuxeo.ecm.platform.gwt.client.ui.Container;
import org.nuxeo.ecm.platform.gwt.client.ui.Site;
import org.nuxeo.ecm.platform.gwt.client.ui.SiteEventHandler;

import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.types.VisibilityMode;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.layout.SectionStack;
import com.smartgwt.client.widgets.layout.SectionStackSection;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class StackContainer implements Container {

    protected SiteEventHandler eventHandler;
    protected SectionStack stack = null;

    public StackContainer() {
        stack = new SectionStack();
        stack.setVisibilityMode(VisibilityMode.MULTIPLE);
        stack.setOverflow(Overflow.HIDDEN);
        stack.setAnimateSections(Boolean.parseBoolean(Framework.getSetting("animations", "false")));
        stack.setHeight100();
        stack.setShowResizeBar(true);
    }

    public String getHandle(Site site) {
        return (String)site.getHandle();
    }

    public SectionStackSection getSection(Site site) {
        //return stack.get getHandle(site);
        return null;
    }

    public SectionStack getWidget() {
        return stack;
    }

    public void activateSite(Site site) {
        stack.expandSection(getHandle(site));
    }

    public void deactivateSite(Site site) {
        stack.collapseSection(getHandle(site));
    }

    public void enableSite(Site site) {
        stack.showSection(getHandle(site));
    }

    public void disableSite(Site site) {
        stack.hideSection(getHandle(site));
    }

    public boolean isSiteActive(Site site) {
        return false;
    }

    public boolean isSiteEnabled(Site site) {
        // TODO Auto-generated method stub
        return false;
    }


    public Object getActiveSiteHandle() {
        return null; //TODO
    }

    public Object createHandle(Site site) {
        SectionStackSection section = new SectionStackSection(); // TODO smartgwt bug: ID is never initialized
        section.setID(SC.generateID());
        boolean isExpanded = SmartClient.getSectionsCount(stack) == 0;
        section.setExpanded(isExpanded); // expand first section
        section.setCanCollapse(true);
        stack.addSection(section);
        return section.getID();
    }

    public void closeSite(Site site) {
        stack.removeSection(getHandle(site));
    }

    public void updateSiteIcon(Site site) {
        String icon = site.getIcon();
        if (icon != null) {
            SmartClient.setSectionIcon(stack, getHandle(site), icon);
        }
    }

    public void updateSiteTitle(Site site) {
        String title = site.getTitle();
        if (title != null) {
            SmartClient.setSectionTitle(stack, getHandle(site), title);
        }
    }



    public void installWidget(Site site) {
        String id = getHandle(site);
        SmartClient.addSectionItem(stack, id, SmartClient.toCanvas(site.getView().getWidget()));
        // force an expand otherwise it will not work
        if (stack.getSectionNumber(id) == 0) {
            stack.expandSection(id);
        }
    }

    public void closeAll() {
        // TODO Auto-generated method stub

    }

    public void clear() {
        // do nothing
    }

    public SiteEventHandler getSiteEventHandler() {
        return eventHandler;
    }

    public void setSiteEventHandler(SiteEventHandler handler) {
        eventHandler = handler;
    }


//    public class SectionSite implements Site {
//        protected String id;
//        protected String title;
//        protected String icon;
//
//        public SectionSite() {
//        }
//        public String getId() {
//            return id;
//        }
//        public void setIcon(String icon) {
//            if (id == null) {
//                this.icon = icon;
//            } else {
//                SmartClient.setSectionIcon(stack, id, title);
//            }
//        }
//        public void setTitle(String title) {
//            if (id == null) {
//                this.title = title;
//            } else {
//                SmartClient.setSectionTitle(stack, id, title);
//            }
//        }
//        public void setHostedWidget(Widget widget) {
//            SectionStackSection section = new SectionStackSection(); // TODO smartgwt bug: ID is never initialized
//            section.setID(SC.generateID());
//            section.setTitle(title);
//            section.addItem(SmartClient.toCanvas(widget));
//            section.setExpanded(SmartClient.getSectionsCount(stack) == 0); // expand first section
//            section.setCanCollapse(true);
//            id = section.getID();
//            stack.addSection(section);
//        }
//    }



}
