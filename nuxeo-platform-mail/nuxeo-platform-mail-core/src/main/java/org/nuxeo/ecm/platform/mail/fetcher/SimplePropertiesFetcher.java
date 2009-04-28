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
 *     Alexandre Russel
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.mail.fetcher;

import java.util.Map;
import java.util.Properties;

/**
 * A simple properties fetcher that always returns the same properties.
 *
 * @author Alexandre Russel
 */
public class SimplePropertiesFetcher implements PropertiesFetcher {

    private final Properties properties = new Properties();

    public void configureFetcher(Map<String, String> configuration) {
        properties.putAll(configuration);
    }

    public Properties getProperties(Map<String, Object> values) {
        return properties;
    }

}
