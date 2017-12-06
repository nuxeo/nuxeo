/*
 * (C) Copyright 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 */

package org.nuxeo.connect.client.status;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.nuxeo.connect.connector.http.ConnectUrlConfig;
import org.nuxeo.connect.data.DownloadablePackage;
import org.nuxeo.connect.data.SubscriptionStatusType;
import org.nuxeo.connect.identity.LogicalInstanceIdentifier;
import org.nuxeo.connect.identity.LogicalInstanceIdentifier.NoCLID;
import org.nuxeo.connect.packages.PackageManager;
import org.nuxeo.connect.packages.dependencies.TargetPlatformFilterHelper;
import org.nuxeo.connect.update.PackageType;
import org.nuxeo.ecm.admin.runtime.PlatformVersionHelper;
import org.nuxeo.runtime.api.Framework;

/**
 * Store information about registration and possible updates
 *
 * @author Tiry (tdelprat@nuxeo.com)
 */
public class ConnectUpdateStatusInfo {

    protected static final String UNREGISTERED = "unregistered";

    protected static final String ONLINE_REGISTERED = "onlineregistered";

    protected static final String CONNECT_UNREACHABLE = "unreachable";

    protected static final String EXPIRED = "expired";

    protected String type;

    protected String bannerPath;

    protected Integer availableUpdateCount;

    protected String feedUrl;

    protected boolean registered;

    protected static Log log = LogFactory.getLog(ConnectUpdateStatusInfo.class);

    public static ConnectUpdateStatusInfo unregistered() {
        ConnectUpdateStatusInfo status = new ConnectUpdateStatusInfo();
        status.type = UNREGISTERED;
        status.setBannerPath("clientSideBanner");
        status.feedUrl = buildFeedUrl(false);
        status.availableUpdateCount = 0;
        status.registered = false;
        return status;
    }

    public static ConnectUpdateStatusInfo ok() {
        ConnectUpdateStatusInfo status = new ConnectUpdateStatusInfo();
        status.type = ONLINE_REGISTERED;
        status.registered = true;
        return status;
    }

    public static ConnectUpdateStatusInfo connectServerUnreachable() {
        ConnectUpdateStatusInfo status = new ConnectUpdateStatusInfo();
        status.type = CONNECT_UNREACHABLE;
        status.setBannerPath("clientSideBanner");
        status.feedUrl = buildFeedUrl(true);
        status.availableUpdateCount = 0;
        status.registered = true;
        return status;
    }

    public static ConnectUpdateStatusInfo notValid() {
        ConnectUpdateStatusInfo status = new ConnectUpdateStatusInfo();
        status.type = EXPIRED;
        status.setBannerPath("serverSideBanner");
        status.registered = true;
        return status;
    }

    protected static String buildFeedUrl(boolean registred) {

        StringBuffer sb = new StringBuffer();

        sb.append(Framework.getProperty("org.nuxeo.connect.client.feedUrl", ConnectUrlConfig.getBaseUrl()));
        sb.append("connect-gateway/jsonp/");

        if (registred) {
            sb.append("registered");
        } else {
            sb.append("unregistered");
        }

        sb.append("?product=");
        sb.append(PlatformVersionHelper.getPlatformFilter());
        if (registred) {
            sb.append("&instance=");
            try {
                sb.append(LogicalInstanceIdentifier.instance().getCLID1());
            } catch (NoCLID e) {
                log.error("Error in ConnectUpdateStatusInfo generation : No CLID is defined ...");
            }
        }

        sb.append("&callback=displayConnectUpdateStatus");
        return sb.toString();
    }

    public String getIdentifier() {
        try {
            return LogicalInstanceIdentifier.instance().getCLID1();
        } catch (NoCLID e) {
            return "";
        }
    }

    public String getDistributionLabel() {
        return PlatformVersionHelper.getDistributionName().toUpperCase() + " "
                + PlatformVersionHelper.getDistributionVersion();
    }

    public String getDistributionName() {
        return PlatformVersionHelper.getDistributionName().toUpperCase();
    }

    public String getDistributionVersion() {
        return PlatformVersionHelper.getDistributionVersion();
    }

    protected int computeAvailableUpdateCount() {
        if (ConnectStatusHolder.instance().getStatus().isConnectServerUnreachable()) {
            return 0;
        }
        PackageManager pm = Framework.getService(PackageManager.class);
        String targetPlatform = PlatformVersionHelper.getPlatformFilter();

        List<DownloadablePackage> pkgs = pm.listUpdatePackages(PackageType.HOT_FIX, targetPlatform);

        List<DownloadablePackage> localHotFixes = pm.listLocalPackages(PackageType.HOT_FIX);

        List<DownloadablePackage> applicablePkgs = new ArrayList<>();

        for (DownloadablePackage pkg : pkgs) {
            if (TargetPlatformFilterHelper.isCompatibleWithTargetPlatform(pkg, targetPlatform)) {
                boolean isInstalled = false;
                for (DownloadablePackage localPkg : localHotFixes) {
                    if (localPkg.getId().equals(pkg.getId())) {
                        isInstalled = true;
                        break;
                    }
                }
                if (!isInstalled) {
                    applicablePkgs.add(pkg);
                }
            }
        }
        return applicablePkgs.size();
    }

    public String getBannerPath() {
        return bannerPath;
    }

    protected void setBannerPath(String bannerName) {
        if (!bannerName.startsWith("/")) {
            bannerPath = "/incl/" + bannerName;
        } else {
            bannerPath = bannerName;
        }
        if (!bannerPath.endsWith(".xhtml")) {
            bannerPath = bannerPath + ".xhtml";
        }
    }

    public int getAvailableUpdateCount() {
        if (availableUpdateCount == null) {
            availableUpdateCount = computeAvailableUpdateCount();
        }
        return availableUpdateCount;
    }

    public String getType() {
        return type;
    }

    public String getFeedUrl() {
        return feedUrl;
    }

    public boolean isRegistered() {
        return registered;
    }

    public boolean isExpired() {
        return ConnectStatusHolder.instance().getStatus().status() == SubscriptionStatusType.EXPIRED;
    }
}
