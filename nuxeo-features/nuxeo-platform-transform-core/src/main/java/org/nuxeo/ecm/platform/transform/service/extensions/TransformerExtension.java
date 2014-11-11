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
 * $Id: TransformerExtension.java 18427 2007-05-09 12:56:23Z sfermigier $
 */

package org.nuxeo.ecm.platform.transform.service.extensions;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.w3c.dom.Element;

/**
 *
 * @author janguenot
 */
@XObject("transformer")
public class TransformerExtension {

    @XNode("@name")
    private String name;

    @XNode("@class")
    private String className;

    @XNode("plugins")
    private Element plugins;

    private Map<String, Map<String, Serializable>> options = new HashMap<String, Map<String, Serializable>>();

    public TransformerExtensionPluginsConfiguration getPluginsChain() {
        TransformerExtensionPluginsConfiguration conf = new TransformerExtensionPluginsConfiguration();
        conf.setElement(plugins);
        return conf;
    }

    public String getClassName() {
        return className;
    }

    public String getName() {
        return name;
    }

    public Map<String, Map<String, Serializable>> getOptions() {
        return options;
    }

}
