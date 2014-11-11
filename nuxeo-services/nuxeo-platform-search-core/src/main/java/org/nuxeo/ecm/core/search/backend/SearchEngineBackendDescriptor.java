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
 * $Id:SearchEnginePluginDescriptor.java 13121 2007-03-01 18:07:58Z janguenot $
 */

package org.nuxeo.ecm.core.search.backend;

import java.io.Serializable;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.core.search.api.backend.SearchEngineBackend;

/**
 * Search engine plugin descriptor.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
@XObject("searchEngineBackend")
public class SearchEngineBackendDescriptor implements Serializable {

    private static final long serialVersionUID = -8610124864397534204L;

    @XNode("@name")
    protected String name;

    @XNode("@default")
    protected boolean isDefault;

    @XNode("configurationFileName")
    protected String configurationFileName;

    @XNode("@class")
    protected Class<SearchEngineBackend> klass;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Class<SearchEngineBackend> getKlass() {
        return klass;
    }

    public void setKlass(Class<SearchEngineBackend> klass) {
        this.klass = klass;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public void setDefault(boolean isDefault) {
        this.isDefault = isDefault;
    }

    public String getConfigurationFileName() {
        return configurationFileName;
    }

    public void setConfigurationFileName(String configurationFileName) {
        this.configurationFileName = configurationFileName;
    }

}
