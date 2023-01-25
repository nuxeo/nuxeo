/*
 * (C) Copyright 2010-2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 */
package org.nuxeo.ecm.admin.runtime;

import org.nuxeo.common.Environment;
import org.nuxeo.connect.platform.PlatformId;
import org.nuxeo.runtime.api.Framework;

public class PlatformVersionHelper {

    public static final String UNKNOWN = "Unknown";

    public static String getApplicationName() {
        return Framework.getProperty(Environment.PRODUCT_NAME, UNKNOWN);
    }

    public static String getApplicationVersion() {
        return Framework.getProperty(Environment.PRODUCT_VERSION, UNKNOWN);
    }

    public static PlatformId getPlatformId() {
        if (getDistributionName().equals(UNKNOWN) || getDistributionVersion().equals(UNKNOWN)) {
            return null;
        }
        return PlatformId.parse(getDistributionName(), getDistributionVersion());
    }

    public static String getDistributionName() {
        return Framework.getProperty(Environment.DISTRIBUTION_NAME, UNKNOWN);
    }

    public static String getDistributionVersion() {
        return Framework.getProperty(Environment.DISTRIBUTION_VERSION, UNKNOWN);
    }

    public static String getDistributionDate() {
        return Framework.getProperty(Environment.DISTRIBUTION_DATE, UNKNOWN);
    }

    public static String getDistributionHost() {
        return Framework.getProperty(Environment.DISTRIBUTION_SERVER, UNKNOWN);
    }
}
