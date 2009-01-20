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
 * $Id: TransformerExtensionPluginsConfiguration.java 19065 2007-05-21 15:39:52Z sfermigier $
 */
package org.nuxeo.ecm.platform.transform.service.extensions;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Object holding the plugins configuration for a transformer.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public class TransformerExtensionPluginsConfiguration {

    private static final String TAG_PLUGIN = "plugin";

    private static final String TAG_PLUGIN_NAME = "name";

    private static final String TAG_OPTION = "option";

    private static final String ATTR_KEY = "name";

    private Element element;

    public Element getElement() {
        return element;
    }

    public void setElement(Element element) {
        this.element = element;
    }

    public List<String> getPluginsChain() {
        List<String> rplugins = new ArrayList<String>();
        NodeList elements = element.getElementsByTagName("plugin");
        int len = elements.getLength();
        for (int i = 0; i < len; i++) {
            Element element = (Element) elements.item(i);
            rplugins.add(element.getAttribute("name"));
        }
        return rplugins;
    }

    public Map<String, Map<String, Serializable>> getDefaultPluginOptions() {

        Map<String, Map<String, Serializable>> allOptions =
                new HashMap<String, Map<String, Serializable>>();

        NodeList plugins = element.getElementsByTagName(TAG_PLUGIN);
        int lenList = plugins.getLength();
        for (int i = 0; i < lenList; i++) {
            Map<String, Serializable> pluginOptions = new HashMap<String, Serializable>();

            Element plugin = (Element) plugins.item(i);
            NodeList options = plugin.getElementsByTagName(TAG_OPTION);

            for (int j = 0, lenSub = options.getLength(); j < lenSub; j++) {
                Element option = (Element) options.item(j);
                if (element != null) {
                    try {
                        String key = option.getAttribute(ATTR_KEY);
                        String value = option.getTextContent().trim();
                        if (key == null) {
                            throw new Exception("Key is unknown");
                        } else if (value == null) {
                            throw new Exception("Value is unknown");
                        } else {
                            pluginOptions.put(key, value);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            allOptions.put(plugin.getAttribute(TAG_PLUGIN_NAME), pluginOptions);
        }
        return allOptions;
    }

}
