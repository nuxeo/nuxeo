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

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * Config file for ShibbolethGroupsService
 *
 * @author <a href="mailto:akervern@nuxeo.com">Arnaud Kervern</a>
 * @see org.nuxeo.ecm.platform.shibboleth.web.service.ShibbolethGroupsService
 */
@XObject("config")
public class ShibbolethGroupsConfig {

    @XNode("parseString")
    protected String parseString;

    @XNode("basePath")
    protected String shibbGroupBasePath;

    public String getParseString() {
        return parseString;
    }

    public void setParseString(String parseString) {
        this.parseString = parseString;
    }

    public String getShibbGroupBasePath() {
        return shibbGroupBasePath;
    }

    public void setShibbGroupBasePath(String shibbGroupBasePath) {
        this.shibbGroupBasePath = shibbGroupBasePath;
    }
}
