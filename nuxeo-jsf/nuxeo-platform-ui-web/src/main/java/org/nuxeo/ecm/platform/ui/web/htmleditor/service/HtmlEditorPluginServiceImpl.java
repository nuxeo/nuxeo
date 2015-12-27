/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
public class HtmlEditorPluginServiceImpl extends DefaultComponent implements HtmlEditorPluginService {

    public static final String PLUGINS_EXTENSION_POINT = "plugins";

    private Map<String, HtmlEditorPluginDescriptor> pluginsDescriptors;

    @Override
    public void activate(ComponentContext context) {
        pluginsDescriptors = new HashMap<String, HtmlEditorPluginDescriptor>();
    }

    @Override
    public void deactivate(ComponentContext context) {
        pluginsDescriptors = null;
    }

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
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
    public void unregisterContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (PLUGINS_EXTENSION_POINT.equals(extensionPoint)) {
            final HtmlEditorPluginDescriptor descriptor = (HtmlEditorPluginDescriptor) contribution;
            pluginsDescriptors.remove(descriptor.getPluginName());
        }
    }

    @Override
    public List<String> getPluginsName() {
        return new ArrayList<String>(pluginsDescriptors.keySet());
    }

    @Override
    public String getFormattedPluginsNames() {
        return StringUtils.join(getPluginsName(), ',');
    }

    public List<String> getToolbarsButtonsNames() {
        return new ArrayList<String>(pluginsDescriptors.keySet());
    }

    @Override
    public String getFormattedToolbarsButtonsNames() {
        return StringUtils.join(getToolbarsButtonsNames(), ',');
    }

    @Override
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
