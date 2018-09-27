/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Thomas Roger
 */

package org.nuxeo.wopi;

import java.util.Set;

/**
 * WOPI info for a given blob xpath.
 *
 * @since 10.3
 */
public class WOPIBlobInfo {

    /**
     * The xpath of the blob.
     */
    public final String xpath;

    /**
     * Set of WOPI actions supported for the blob.
     */
    public final Set<String> actions;

    /**
     * Application name used for the blob.
     * <p>
     * For instance, Excel, Word, PowerPoint, ...
     */
    public final String appName;

    public WOPIBlobInfo(String xpath, String appName, Set<String> actions) {
        this.xpath = xpath;
        this.actions = actions;
        this.appName = appName;
    }
}
