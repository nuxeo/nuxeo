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
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.webapp.querydata;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.model.Extension;

@Deprecated
public class QueryDataService extends DefaultComponent implements
        QueryDataServiceCommon {

    public static final ComponentName NAME = new ComponentName(
            "queryDataComponentBase");

    private static final Log log = LogFactory.getLog(QueryDataService.class);

    private QueryExtensionPointHandler queryExtensionHandler;

    private DisplayExtensionPointHandler displayExtensionHandler;

    private Registry querysRegistry;

    private Registry displaysRegistry;

    public QueryDataService() {
        queryExtensionHandler = new QueryExtensionPointHandler();
        displayExtensionHandler = new DisplayExtensionPointHandler();

        querysRegistry = new Registry("Query" + QueryDataService.class.getName());
        displaysRegistry = new Registry("Display" + QueryDataService.class.getName());
    }

    @Override
    public void activate(ComponentContext context) throws Exception {
        // TODO: put initialization here! not in ctor
    }

    @Override
    public void deactivate(ComponentContext context) throws Exception {
        // TODO: shutdown registries here
    }

    @Override
    public void registerExtension(Extension extension) {
        if (extension.getExtensionPoint().equals("queryplugins")) {
            queryExtensionHandler.registerExtension(extension);
        } else if (extension.getExtensionPoint().equals("displayplugins")) {
            displayExtensionHandler.registerExtension(extension);
        } else {
            log.error("Unknown contributions... can't register !");
        }
    }

    @Override
    public void unregisterExtension(Extension extension) {
        if (extension.getExtensionPoint().equals("queryplugins")) {
            queryExtensionHandler.unregisterExtension(extension);
        } else if (extension.getExtensionPoint().equals("displayplugins")) {
            displayExtensionHandler.unregisterExtension(extension);
        } else {
            log.error("Unknown contributions... can't unregister !");
        }
    }

    private Registry getDisplayRegistry() {
        return displaysRegistry;
    }

    private Registry getQueryRegistry() {
        return querysRegistry;
    }

    public DisplayPlugin getDisplayPluginByName(String name) {
        return (DisplayPlugin) displaysRegistry.getObjectByName(name);
    }

    public QueryPlugin getQueryPluginByName(String name) {
        return (QueryPlugin) querysRegistry.getObjectByName(name);
    }

    public void registerDisplayPlugin(String name, DisplayPlugin plugin) {
        displaysRegistry.register(name, plugin);
        log.info("DisplayPlugin : " + name + " has been registered !");
    }

    public void registerQueryPlugin(String name, QueryPlugin plugin) {
        querysRegistry.register(name, plugin);
        log.info("QueryPlugin : " + name + " has been registered !");
    }

    public void unregisterDisplayPlugin(String name) {
        displaysRegistry.unregister(name);
        log.info("DisplayPlugin : " + name + " has been unregistered !");
    }

    public void unregisterQueryPlugin(String name) {
        querysRegistry.unregister(name);
        log.info("QueryPlugin : " + name + " has been unregistered !");
    }

}
