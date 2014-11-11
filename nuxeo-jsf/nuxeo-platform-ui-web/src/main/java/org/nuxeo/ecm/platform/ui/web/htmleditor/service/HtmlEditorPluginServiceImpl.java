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
 *     troger
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.ui.web.htmleditor.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.common.utils.StringUtils;
import org.nuxeo.ecm.platform.ui.web.htmleditor.api.HtmlEditorPluginService;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Service used to register plugins for TinyMCE.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
public class HtmlEditorPluginServiceImpl extends DefaultComponent implements
        HtmlEditorPluginService {

    public static final String PLUGINS_EXTENSION_POINT = "plugins";

    private Map<String, HtmlEditorPluginDescriptor> pluginsDescriptors;

    @Override
    public void activate(ComponentContext context) throws Exception {
        pluginsDescriptors = new HashMap<String, HtmlEditorPluginDescriptor>();
    }

    @Override
    public void deactivate(ComponentContext context) throws Exception {
        pluginsDescriptors = null;
    }

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor) {
        if (PLUGINS_EXTENSION_POINT.equals(extensionPoint)) {
            final HtmlEditorPluginDescriptor descriptor = (HtmlEditorPluginDescriptor) contribution;
            if (descriptor.getRemove() && pluginsDescriptors.containsKey(descriptor.getPluginName())) {
                pluginsDescriptors.remove(descriptor.getPluginName());
            } else {
                pluginsDescriptors.put(descriptor.getPluginName(), descriptor);
            }
        }
    }

    @Override
    public void unregisterContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor) {
        if (PLUGINS_EXTENSION_POINT.equals(extensionPoint)) {
            final HtmlEditorPluginDescriptor descriptor = (HtmlEditorPluginDescriptor) contribution;
            pluginsDescriptors.remove(descriptor.getPluginName());
        }
    }

    public List<String> getPluginsName() {
        return new ArrayList<String>(pluginsDescriptors.keySet());
    }

    public String getFormattedPluginsNames() {
        return StringUtils.join(getPluginsName(), ',');
    }

    public Map<String, String> getToolbarsButtons() {
        final Map<String, String> result = new HashMap<String, String>();
        final Map<String, List<String>> temp = new HashMap<String, List<String>>();

        for (final HtmlEditorPluginDescriptor descriptor : pluginsDescriptors.values()) {
            List<String> buttonsList = temp.get(descriptor.getToolbarName());
            if (buttonsList == null) {
                buttonsList = new ArrayList<String>();
            }
            buttonsList.add(descriptor.getPluginButtonName());
            temp.put(descriptor.getToolbarName(), buttonsList);
        }

        for (final Map.Entry<String, List<String>> entry : temp.entrySet()) {
            result.put(entry.getKey(), StringUtils.join(entry.getValue(), ','));
        }
        return result;
    }

}
