/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.webapp.search;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.search.api.client.querymodel.descriptor.FieldDescriptor;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.model.Extension;

/**
 * @author <a href="mailto:glefter@nuxeo.com">George Lefter</a>
 */
public class SearchUIConfigService extends DefaultComponent {

    public static final ComponentName NAME = new ComponentName(SearchUIConfigService.class.getName());

    private static final Log log = LogFactory.getLog(SearchUIConfigService.class);

    private Map<String, ConfigDescriptor> configMap;

    private Map<String, List<FieldDescriptor>> resultColumnsMap;

    public static final String AVAILABLE_SEARCH_COLUMNS = "availableSearchColumns";

    public static final String AVAILABLE_RESULT_COLUMNS = "availableResultColumns";

    @Override
    public void activate(ComponentContext context) throws Exception {
        log.debug("<activate>");
        configMap = new HashMap<String, ConfigDescriptor>();
        resultColumnsMap = new HashMap<String, List<FieldDescriptor>>();
        super.activate(context);
    }

    @Override
    public void deactivate(ComponentContext context) throws Exception {
        log.debug("<deactivate>");
        configMap = null;
        super.deactivate(context);
    }

    @Override
    public void registerExtension(Extension extension) throws Exception {
        String extensionPoint = extension.getExtensionPoint();
        if (extensionPoint.equals("config")) {
            registerConfig(extension);
        } else if (extensionPoint.equals("resultColumns")){
            registerResultColumnGroup(extension);
        } else {
            throw new Exception("unknown extension point: " + extensionPoint);
        }
    }

    public void registerConfig(Extension extension) {
        Object[] contribs = extension.getContributions();
        for (Object contrib : contribs) {
            ConfigDescriptor config = (ConfigDescriptor) contrib;
            configMap.put(config.getName(), config);

            log.debug("registered service config: " + config.name);
        }
    }

    public void registerResultColumnGroup(Extension extension) {
        Object[] contribs = extension.getContributions();
        for (Object contrib : contribs) {
            ResultColumnsDescriptor desc = (ResultColumnsDescriptor) contrib;
            resultColumnsMap.put(desc.getName(), desc.getFields());

            log.debug("registered column group: " + desc.getName());
        }
    }

    @Override
    public void unregisterExtension(Extension extension) throws Exception {
        Object[] contribs = extension.getContributions();
        for (Object contrib : contribs) {
            if (extension.getExtensionPoint().equals("config")) {
                ConfigDescriptor config = (ConfigDescriptor) contrib;
                configMap.put(config.getName(), null);
                log.debug("unregistered service config: " + config.getName());
            } else if (extension.getExtensionPoint().equals("resultColumns")){
                ResultColumnsDescriptor config = (ResultColumnsDescriptor) contrib;
                resultColumnsMap.put(config.getName(), null);
                log.debug("unregistered service config: " + config.getName());
            }

        }
    }

    public List<FieldGroupDescriptor> getFieldGroups(String configName) {
        ConfigDescriptor config = configMap.get(configName);
        if(config == null) {
            log.error("unknown config name: " + configName);
            return null;
        }
        return config.getFieldGroups();
    }

    public List<FieldDescriptor> getResultColumns(String name) {
        return resultColumnsMap.get(name);
    }

}
