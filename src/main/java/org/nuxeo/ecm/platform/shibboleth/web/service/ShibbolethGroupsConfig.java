/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
