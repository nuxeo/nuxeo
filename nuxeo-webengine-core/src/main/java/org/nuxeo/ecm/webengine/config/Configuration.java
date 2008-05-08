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

package org.nuxeo.ecm.webengine.config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.StringUtils;
import org.nuxeo.ecm.webengine.WebRoot;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class Configuration {

    private static final Log log = LogFactory.getLog(Configuration.class);

    final Map<String, Configurator> configurators = new HashMap<String, Configurator>();

    final String mainSection;

    public Configuration() {
        this("main");
    }

    public Configuration(String mainSection) {
        this.mainSection = mainSection;
    }

    public void putConfigurator(String name, Configurator cfg) {
        configurators.put(name, cfg);
    }

    public Configurator getConfigurator(String name) {
        return configurators.get(name);
    }

    public void loadConfiguration(WebRoot root, File file) throws IOException {
        Map<String,String> properties = new HashMap<String, String>();
        BufferedReader reader = new BufferedReader(new FileReader(file));
        String line = reader.readLine();
        Configurator section = configurators.get(mainSection);
        if (section == null) {
            log.warn("Unknown configuration section: "+mainSection);
        }
        while (line != null) {
            line = line.trim();
            if (line.startsWith("#")) {
                line = reader.readLine();
                continue;
            }
            if (line.startsWith("[") && line.endsWith("]")) {
                if (!properties.isEmpty() && section != null) {
                    section.configure(root, properties);
                }
                properties = new HashMap<String, String>();
                String sectionName = line.substring(1, line.length()-1);
                section = configurators.get(sectionName);
                if (section == null) {
                    log.warn("Unknown configuration section: "+sectionName);
                }
            } else if (line.length() != 0){
                String[] ar = StringUtils.split(line, '=', true);
                properties.put(ar[0], ar[1]);
            }
            line = reader.readLine();
        }
        if (!properties.isEmpty() && section != null) {
            section.configure(root, properties);
        }
    }

}
