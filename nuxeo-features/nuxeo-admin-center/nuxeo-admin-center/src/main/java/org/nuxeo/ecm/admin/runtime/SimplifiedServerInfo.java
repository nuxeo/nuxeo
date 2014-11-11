/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.admin.runtime;

import java.util.List;

/**
 * Holds information about current deployed Nuxeo Platform
 *
 * @author tiry
 */
public class SimplifiedServerInfo {

    protected List<SimplifiedBundleInfo> bundleInfos;

    protected String runtimeVersion;

    protected List<String> warnings;


    public List<SimplifiedBundleInfo> getBundleInfos() {
        return bundleInfos;
    }

    public void setBundleInfos(List<SimplifiedBundleInfo> bundleInfos) {
        this.bundleInfos = bundleInfos;
    }

    public String getApplicationName() {
        return PlatformVersionHelper.getApplicationName();
    }

    public String getApplicationVersion() {
        return PlatformVersionHelper.getApplicationVersion();
    }

    public String getDistributionName() {
        return PlatformVersionHelper.getDistributionName();
    }

    public String getDistributionVersion() {
        return PlatformVersionHelper.getDistributionVersion();
    }

    public String getDistributionHost() {
        return PlatformVersionHelper.getDistributionHost();
    }

    public String getDistributionDate() {
        return PlatformVersionHelper.getDistributionDate();
    }

    public String getRuntimeVersion() {
        return runtimeVersion;
    }

    public void setRuntimeVersion(String runtimeVersion) {
        this.runtimeVersion = runtimeVersion;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
    }

    public boolean hasWarnings() {
        return warnings != null && !warnings.isEmpty();
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();

        sb.append(getApplicationName());
        sb.append("  ");
        sb.append(getApplicationVersion());
        sb.append("\n");

        sb.append("runtime :  ");
        sb.append(runtimeVersion);
        sb.append("\n");

        sb.append("warnings :  ");
        if (warnings == null || warnings.isEmpty()) {
            sb.append("none");
        } else {
            for (String warn : warnings) {
                sb.append("\n  ");
                sb.append(warn);
            }
        }

        sb.append("\nbundles :  ");
        for (SimplifiedBundleInfo bi : bundleInfos) {
            sb.append("\n  ");
            sb.append(bi.getName());
            sb.append("    (");
            sb.append(bi.getVersion());
            sb.append(")");
        }

        return sb.toString();
    }
}
