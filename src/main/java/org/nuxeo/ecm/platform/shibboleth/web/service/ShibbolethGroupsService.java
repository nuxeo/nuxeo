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

/**
 * Interface handling Shibboleth Groups Service methods
 *
 * @author <a href="mailto:akervern@nuxeo.com">Arnaud Kervern</a>
 */
public interface ShibbolethGroupsService {
    /**
     * Get the defined string to be used as hierarchy delimiter. Example : As ":" defined as the hierarchy delimiter.
     * group:name:student
     *
     * @return the string to use
     */
    String getParseString();

    /**
     * Get the base path (using the previously demlimiter) where shibb group will be stored
     *
     * @return
     */
    String getShibbGroupBasePath();
}
