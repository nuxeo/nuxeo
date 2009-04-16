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
 * $Id:AbstractSearchEngineBackend.java 13117 2007-03-01 17:43:28Z janguenot $
 */

package org.nuxeo.ecm.core.search.api.backend.impl;

import java.util.Collections;
import java.util.List;

import org.nuxeo.ecm.core.search.api.backend.SearchEngineBackend;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Abstract search engine backend.
 * <p>
 * Third party code that whish to register a backend must extend this abc.
 *
 * @see org.nuxeo.ecm.core.search.api.backend.SearchEngineBackend
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public abstract class AbstractSearchEngineBackend extends DefaultComponent implements
        SearchEngineBackend {

    private static final long serialVersionUID = -430131396562337751L;

    protected String name;

    protected String configurationFileName;

    protected final List<String> supportedAnalyzers = Collections.emptyList();

    protected final List<String> supportedFieldTypes = Collections.emptyList();


    protected AbstractSearchEngineBackend() {
    }

    protected AbstractSearchEngineBackend(String name) {
        this.name = name;
    }

    protected AbstractSearchEngineBackend(String name,
            String configurationFileName) {
        this.name = name;
        this.configurationFileName = configurationFileName;
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getSupportedAnalyzersFor() {
        return supportedAnalyzers;
    }

    public List<String> getSupportedFieldTypes() {
        return supportedFieldTypes;
    }

    public String getConfigurationFileName() {
        return configurationFileName;
    }

    public void setConfigurationFileName(String configurationFileName) {
        this.configurationFileName = configurationFileName;
    }

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor) {
        // do nothing by default. Most backends won't require extra
        // nxruntime configuration
    }

}
