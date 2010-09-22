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

    public List<SimplifiedBundleInfo> getBundleInfos() {
        return bundleInfos;
    }

    public void setBundleInfos(List<SimplifiedBundleInfo> bundleInfos) {
        this.bundleInfos = bundleInfos;
    }

    public String getPlatformName() {
        return platformName;
    }

    public void setPlatformName(String platformName) {
        this.platformName = platformName;
    }

    public String getPlatformVersion() {
        return platformVersion;
    }

    public void setPlatformVersion(String platformVersion) {
        this.platformVersion = platformVersion;
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

    protected List<SimplifiedBundleInfo> bundleInfos;

    protected String platformName;

    protected String platformVersion;

    protected String runtimeVersion;

    protected List<String> warnings;

    public boolean hasWarnings() {
        return (warnings != null && warnings.size() > 0);
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();

        sb.append(platformName);
        sb.append("  ");
        sb.append(platformVersion);
        sb.append("\n");

        sb.append("runtime :  ");
        sb.append(runtimeVersion);
        sb.append("\n");

        sb.append("warnings :  ");
        if (warnings == null | warnings.size() == 0) {
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
