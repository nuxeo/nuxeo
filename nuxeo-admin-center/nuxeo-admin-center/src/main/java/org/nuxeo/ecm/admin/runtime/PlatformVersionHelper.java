/*
 * (C) Copyright 2010-2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 */
package org.nuxeo.ecm.admin.runtime;

import org.nuxeo.connect.connector.fake.FakeDownloadablePackage;
import org.nuxeo.connect.packages.dependencies.TargetPlatformFilterHelper;
import org.nuxeo.connect.update.Version;
import org.nuxeo.runtime.api.Framework;

public class PlatformVersionHelper {

    public static final String UNKNOWN = "Unknown";

    public static String getApplicationName() {
        return Framework.getProperty("org.nuxeo.ecm.product.name", UNKNOWN);
    }

    public static String getApplicationVersion() {
        return Framework.getProperty("org.nuxeo.ecm.product.version", UNKNOWN);
    }

    public static String getPlatformFilter() {
        if (getDistributionName().equals(UNKNOWN)) {
            return null;
        }
        return getDistributionName() + "-" + getDistributionVersion();
    }

    public static String getDistributionName() {
        return Framework.getProperty("org.nuxeo.distribution.name", UNKNOWN);
    }

    public static String getDistributionVersion() {
        return Framework.getProperty("org.nuxeo.distribution.version", UNKNOWN);
    }

    public static String getDistributionDate() {
        return Framework.getProperty("org.nuxeo.distribution.date", UNKNOWN);
    }

    public static String getDistributionHost() {
        return Framework.getProperty("org.nuxeo.distribution.server", UNKNOWN);
    }

    /**
     * @deprecated Since 5.9.6. Badly duplicates
     *             {@link TargetPlatformFilterHelper#isCompatibleWithTargetPlatform(org.nuxeo.connect.update.Package, String)}
     */
    @Deprecated
    public static boolean isCompatible(final String[] targetPlatforms2,
            String currentPlatform) {
        return TargetPlatformFilterHelper.isCompatibleWithTargetPlatform(
                new FakeDownloadablePackage("wrapper", Version.ZERO) {
                    @Override
                    public String[] getTargetPlatforms() {
                        return targetPlatforms2;
                    }
                }, currentPlatform);
    }

    @Deprecated
    public static boolean isCompatible(String[] targetPlatforms) {
        return isCompatible(targetPlatforms, getPlatformFilter());
    }

}
