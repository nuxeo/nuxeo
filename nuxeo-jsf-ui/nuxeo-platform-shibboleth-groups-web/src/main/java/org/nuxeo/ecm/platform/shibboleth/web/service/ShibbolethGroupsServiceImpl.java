/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and others.
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
 * Contributors:
 *     Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.platform.shibboleth.web.service;

import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Shibboleth Group service component implementation
 *
 * @author <a href="mailto:akervern@nuxeo.com">Arnaud Kervern</a>
 */
public class ShibbolethGroupsServiceImpl extends DefaultComponent implements ShibbolethGroupsService {

    public static final String CONFIG_EP = "config";

    protected ShibbolethGroupsConfig config;

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (CONFIG_EP.equals(extensionPoint)) {
            config = (ShibbolethGroupsConfig) contribution;
        }
    }

    @Override
    public String getParseString() {
        return config == null ? null : config.getParseString();
    }

    @Override
    public String getShibbGroupBasePath() {
        return config == null ? null : config.getShibbGroupBasePath();
    }
}
