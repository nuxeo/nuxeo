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
 *
 */

package org.nuxeo.connect.client.jsf;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.contexts.Contexts;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.common.utils.ExceptionUtils;
import org.nuxeo.connect.client.status.ConnectStatusHolder;
import org.nuxeo.connect.client.status.ConnectUpdateStatusInfo;
import org.nuxeo.connect.client.status.SubscriptionStatusWrapper;
import org.nuxeo.connect.connector.NuxeoClientInstanceType;
import org.nuxeo.connect.connector.http.ConnectUrlConfig;
import org.nuxeo.connect.data.SubscriptionStatusType;
import org.nuxeo.connect.identity.LogicalInstanceIdentifier;
import org.nuxeo.connect.identity.LogicalInstanceIdentifier.InvalidCLID;
import org.nuxeo.connect.identity.LogicalInstanceIdentifier.NoCLID;
import org.nuxeo.connect.identity.TechnicalInstanceIdentifier;
import org.nuxeo.connect.registration.ConnectRegistrationService;
import org.nuxeo.connect.update.PackageException;
import org.nuxeo.connect.update.PackageUpdateService;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CloseableFile;
import org.nuxeo.runtime.api.Framework;

/**
 * Seam Bean to expose Connect Registration operations.
 * <ul>
 * <li>getting status
 * <li>registering
 * <li>...
 * </ul>
 *
 * @author <a href="mailto:td@nuxeo.com">Thierry Delprat</a>
 */
@Name("connectStatus")
@Scope(ScopeType.CONVERSATION)
public class ConnectStatusActionBean implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String CLIENT_BANNER_TYPE = "clientSideBanner";

    public static final String SERVER_BANNER_TYPE = "serverSideBanner";

    private static final Log log = LogFactory.getLog(ConnectStatusActionBean.class);

    @In(create = true, required = false)
    protected FacesMessages facesMessages;

    @In(create = true, required = true, value = "appsViews")
    protected AppCenterViewsManager appsViews;

    @In(create = true)
    protected Map<String, String> messages;

    protected String CLID;

    protected String token;

    protected ConnectUpdateStatusInfo connectionStatusCache;

    public String getRegistredCLID() throws NoCLID {
        if (isRegistered()) {
            return LogicalInstanceIdentifier.instance().getCLID();
        } else {
            return null;
        }
    }

    public String getCLID() {
        return CLID;
    }

    public void setCLID(String cLID) {
        CLID = cLID;
    }

    public String unregister() {
        LogicalInstanceIdentifier.cleanUp();
        resetRegister();
        return null;
    }

    public List<SelectItem> getInstanceTypes() {
        List<SelectItem> types = new ArrayList<>();
        for (NuxeoClientInstanceType itype : NuxeoClientInstanceType.values()) {
            SelectItem item = new SelectItem(itype.getValue(), "label.instancetype." + itype.getValue());
            types.add(item);
        }
        return types;
    }

    protected ConnectRegistrationService getService() {
        return Framework.getService(ConnectRegistrationService.class);
    }

    /**
     * @since 9.2
     */
    @Factory(scope = ScopeType.APPLICATION, value = "registredConnectInstance")
    public boolean isRegistered() {
        return getService().isInstanceRegistered();
    }

    /**
     * @deprecated Since 9.2, use {@link #isRegistered()} instead.
     */
    @Deprecated
    public boolean isRegistred() {
        return isRegistered();
    }

    protected void flushContextCache() {
        // A4J and Event cache don't play well ...
        Contexts.getApplicationContext().remove("registredConnectInstance");
        Contexts.getApplicationContext().remove("connectUpdateStatusInfo");
        appsViews.flushCache();
    }

    @Factory(value = "connectServerReachable", scope = ScopeType.EVENT)
    public boolean isConnectServerReachable() {
        return !ConnectStatusHolder.instance().getStatus().isConnectServerUnreachable();
    }

    public String refreshStatus() {
        ConnectStatusHolder.instance().getStatus(true);
        flushContextCache();
        return null;
    }

    public SubscriptionStatusWrapper getStatus() {
        return ConnectStatusHolder.instance().getStatus();
    }

    /**
     * Returns the registration expiration date included in the CLID, or {@code null} if the CLID cannot be loaded or
     * doesn't include the expiration date (old v0 format).
     *
     * @since 10.2
     */
    public Calendar getRegistrationExpirationDate() {
        long timestamp = ConnectStatusHolder.instance().getRegistrationExpirationTimestamp();
        if (timestamp > -1) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(timestamp * 1000);
            return calendar;
        } else {
            return null;
        }
    }

    public String resetRegister() {
        flushContextCache();
        return null;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) throws IOException, InvalidCLID {
        if (token != null) {
            String tokenData = new String(Base64.decodeBase64(token));
            String[] tokenDataLines = tokenData.split("\n");
            for (String line : tokenDataLines) {
                String[] parts = line.split(":");
                if (parts.length > 1 && "CLID".equals(parts[0])) {
                    getService().localRegisterInstance(parts[1], " ");
                    // force refresh of connect status info
                    connectionStatusCache = null;
                    flushContextCache();
                    ConnectStatusHolder.instance().flush();
                }
            }
        }
    }

    public String getCTID() {
        try {
            return TechnicalInstanceIdentifier.instance().getCTID();
        } catch (Exception e) { // stupid API
            throw ExceptionUtils.runtimeException(e);
        }
    }

    public String localRegister() {
        try {
            getService().localRegisterInstance(CLID, "");
        } catch (InvalidCLID e) {
            facesMessages.addToControl("offline_clid", StatusMessage.Severity.WARN,
                    messages.get("label.connect.wrongCLID"));
        } catch (IOException e) {
            facesMessages.addToControl("offline_clid", StatusMessage.Severity.ERROR,
                    messages.get("label.connect.registrationError"));
            log.error("Error while registering instance locally", e);
        }
        flushContextCache();
        return null;
    }

    protected Blob packageToUpload;

    protected String packageFileName;

    public String getPackageFileName() {
        return packageFileName;
    }

    public void setPackageFileName(String packageFileName) {
        this.packageFileName = packageFileName;
    }

    public Blob getPackageToUpload() {
        return packageToUpload;
    }

    public void setPackageToUpload(Blob packageToUpload) {
        this.packageToUpload = packageToUpload;
    }

    public void uploadPackage() throws IOException {
        if (packageToUpload == null) {
            facesMessages.add(StatusMessage.Severity.WARN, "label.connect.nofile");
            return;
        }
        PackageUpdateService pus = Framework.getService(PackageUpdateService.class);
        try (CloseableFile cfile = packageToUpload.getCloseableFile()) {
            pus.addPackage(cfile.getFile());
        } catch (PackageException e) {
            log.warn(e, e);
            facesMessages.add(StatusMessage.Severity.ERROR,
                    messages.get("label.connect.wrong.package") + ":" + e.getMessage());
            return;
        } finally {
            packageFileName = null;
            packageToUpload = null;
        }
    }

    public ConnectUpdateStatusInfo getDynamicConnectUpdateStatusInfo() {
        HttpServletRequest req = (HttpServletRequest) FacesContext.getCurrentInstance()
                                                                  .getExternalContext()
                                                                  .getRequest();
        String bannerType = req.getParameter("bannerType");
        if ("unregistered".equals(bannerType)) {
            return ConnectUpdateStatusInfo.unregistered();
        } else if ("notreachable".equals(bannerType)) {
            return ConnectUpdateStatusInfo.connectServerUnreachable();
        } else if ("notvalid".equals(bannerType)) {
            return ConnectUpdateStatusInfo.notValid();
        } else if ("ok".equals(bannerType)) {
            return ConnectUpdateStatusInfo.ok();
        }
        return getConnectUpdateStatusInfo();
    }

    /**
     * @since 5.9.2
     */
    @Factory(scope = ScopeType.APPLICATION, value = "connectBannerEnabled")
    public boolean isConnectBannerEnabled() {
        final String testerName = Framework.getProperty("org.nuxeo.ecm.tester.name");
        if (testerName != null && testerName.equals("Nuxeo-Selenium-Tester")) {
            // disable banner when running selenium tests
            return false;
        }
        return true;
    }

    @Factory(scope = ScopeType.APPLICATION, value = "connectUpdateStatusInfo")
    public ConnectUpdateStatusInfo getConnectUpdateStatusInfo() {
        if (connectionStatusCache == null) {
            if (!isRegistered()) {
                connectionStatusCache = ConnectUpdateStatusInfo.unregistered();
            } else {
                if (isConnectBannerEnabled() && isConnectServerReachable()) {
                    if (getStatus().isError()) {
                        connectionStatusCache = ConnectUpdateStatusInfo.connectServerUnreachable();
                    } else {
                        if (ConnectStatusHolder.instance().getStatus().status() == SubscriptionStatusType.OK) {
                            connectionStatusCache = ConnectUpdateStatusInfo.ok();
                        } else {
                            connectionStatusCache = ConnectUpdateStatusInfo.notValid();
                        }
                    }
                } else {
                    connectionStatusCache = ConnectUpdateStatusInfo.connectServerUnreachable();
                }
            }
        }
        return connectionStatusCache;
    }

    @Factory("nuxeoConnectUrl")
    public String getConnectServerUrl() {
        return ConnectUrlConfig.getBaseUrl();
    }

}
