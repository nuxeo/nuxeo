/*
 * (C) Copyright 2006-2010 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thierry Delprat
 */
package org.nuxeo.apidoc.documentation;

import org.nuxeo.apidoc.snapshot.SnapshotManager;
import org.nuxeo.runtime.api.Framework;

public class JavaDocHelper {

    public static final String BASE_URL = "http://community.nuxeo.com/api/";

    public static final String DM_BASE = "nuxeo";

    public static final String DEFAULT_DIST = DM_BASE;

    public static final String DEFAULT_VERSION = "5.5";

    protected final String defaultPrefix;

    protected final String docVersion;

    public JavaDocHelper(String prefix, String version) {
        defaultPrefix = prefix;

        if (version.equalsIgnoreCase("current")) {
            SnapshotManager sm = Framework.getService(SnapshotManager.class);
            version = sm.getRuntimeSnapshot().getVersion();
        }

        if (version.endsWith("-SNAPSHOT")) {
            version = version.replace("-SNAPSHOT", "");
        } else {
            version = "release-" + version;
        }
        docVersion = version;
    }

    public String getBaseUrl(String className) {
        String base = defaultPrefix;
        return BASE_URL + base + "/" + docVersion;
    }

    public static JavaDocHelper getHelper(String distribName, String distribVersion) {
        String base = DEFAULT_DIST;
        return new JavaDocHelper(base, distribVersion);
    }

}
