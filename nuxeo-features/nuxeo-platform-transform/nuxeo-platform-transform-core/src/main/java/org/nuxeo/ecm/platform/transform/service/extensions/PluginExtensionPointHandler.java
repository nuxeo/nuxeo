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
 * $Id: PluginExtensionPointHandler.java 28924 2008-01-10 14:04:05Z sfermigier $
 */

package org.nuxeo.ecm.platform.transform.service.extensions;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.platform.transform.api.TransformException;
import org.nuxeo.ecm.platform.transform.interfaces.Plugin;
import org.nuxeo.ecm.platform.transform.interfaces.TransformServiceCommon;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.Extension;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Extension point handler for plugin.
 *
 * @author janguenot
 */
@SuppressWarnings("deprecation")
public class PluginExtensionPointHandler extends
        NXTransformExtensionPointHandler {

    protected static final String TAG_OPTION = "option";

    protected static final String ATTR_KEY = "name";

    public static void unregisterExtension(Extension extension) {
        Object[] contribs = extension.getContributions();

        for (Object contrib : contribs) {
            PluginExtension pluginExtension = (PluginExtension) contrib;

            try {
                unregisterOne(pluginExtension, extension);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    public static void registerExtension(Extension extension) {
        Object[] contribs = extension.getContributions();

        for (Object contrib : contribs) {
            PluginExtension pluginExtension = (PluginExtension) contrib;
            pluginExtension.setDefaultOptions(getDefaultPluginOptions(extension));
            try {
                registerOne(pluginExtension, extension);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static void registerOne(PluginExtension pluginExtension,
            Extension extension) throws Exception {

        String name = pluginExtension.getName();
        List<String> sourceMimeTypes = pluginExtension.getSourceMimeTypes();
        String destinationMimeType = pluginExtension.getDestinationMimeType();
        String className = pluginExtension.getClassName();
        Map<String, Serializable> defaultOptions = pluginExtension
                .getDefaultOptions();

        if (null == className) {
            // TODO: DM
            // this is encountered due to using the same tag name <plugin>
            // for referencing a defined plugin.
            // This is caused probably by a bug in component contributions loading ???
            //
            log.warn("Plugin class not specified for plugin extension: " + pluginExtension);
            return;
        }
        Class<Plugin> pluginClass = (Class<Plugin>) extension.getContext().loadClass(
                className);
        if (null == pluginClass) {
            throw new TransformException("Unable to load plugin class: '" + className + "'");
        }
        Plugin plugin = pluginClass.newInstance();
        plugin.setName(name);
        plugin.setSourceMimeTypes(sourceMimeTypes);
        plugin.setDestinationMimeType(destinationMimeType);
        plugin.setDefaultOptions(defaultOptions);

        TransformServiceCommon transformService = getNXTransform();
        if (transformService != null) {
            transformService.registerPlugin(name, plugin);
        } else {
            throw new TransformException("No TransformServiceCommon service found."
                    + "Impossible to register plugin: " + name);
        }
    }

    private static void unregisterOne(PluginExtension pluginExtension,
            Extension extension) {
        String name = pluginExtension.getName();
        getNXTransform().unregisterPlugin(name);
    }

    /*
     * Extract plugin options.
     *
     */
    // XXX: Can I do this using XNodeList directly ?
    private static Map<String, Serializable> getDefaultPluginOptions(
            Extension extension) {

        Map<String, Serializable> options = new HashMap<String, Serializable>();

        Element root = extension.getElement();
        NodeList list = root.getElementsByTagName(TAG_OPTION);

        for (int i = 0, len = list.getLength(); i < len; i++) {
            Element element = (Element) list.item(i);
            try {
                String key = element.getAttribute(ATTR_KEY);
                String value = element.getTextContent();
                if (key == null) {
                    throw new Exception("Key is unknown");
                } else if (value == null) {
                    throw new Exception("Value is unknown");
                }
                value = value.trim();
                if (value.startsWith("$")) {
                    value = Framework.expandVars(value);
                }
                if (log.isDebugEnabled()) {
                    log.debug("Found option with name=" + key + " and value="
                            + value);
                }
                options.put(key, value);
            } catch (Exception e) {
                log.error("Failed to extract options from plugin extension", e);
            }
        }
        return options;
    }

}
