/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
package org.nuxeo.ecm.platform.ui.web.contentview;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * @author Anahide Tchertchian
 */
public class ContentViewServiceImpl extends DefaultComponent implements
        ContentViewService {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(ContentViewServiceImpl.class);

    public static final String CONTENT_VIEW_EP = "contentViews";

    protected Map<String, ContentView> contentViews = new HashMap<String, ContentView>();

    public ContentView getContentView(String name) {
        return contentViews.get(name);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getAdapter(Class<T> adapter) {
        if (adapter.isAssignableFrom(ContentViewService.class)) {
            return (T) this;
        }
        return null;
    }

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if (CONTENT_VIEW_EP.equals(extensionPoint)) {
            ContentView desc = (ContentViewDescriptor) contribution;
            String name = desc.getName();
            if (name == null) {
                log.error("Cannot register content view without a name");
                return;
            }
            if (contentViews.containsKey(name)) {
                log.info("Overriding content view with name " + name);
            }
            contentViews.put(name, desc);
        }
    }

    @Override
    public void unregisterContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if (CONTENT_VIEW_EP.equals(extensionPoint)) {
            ContentView desc = (ContentViewDescriptor) contribution;
            contentViews.remove(desc.getName());
        }
    }

}
