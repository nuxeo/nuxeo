/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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

    @Override
    public void configureFetcher(Map<String, String> configuration) {
        properties.putAll(configuration);
    }

    @Override
    public Properties getProperties(Map<String, Object> values) {
        return properties;
    }

}
