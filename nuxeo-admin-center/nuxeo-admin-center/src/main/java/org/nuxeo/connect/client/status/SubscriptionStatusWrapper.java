/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 */

package org.nuxeo.connect.client.status;

import java.util.Calendar;

import org.nuxeo.connect.connector.ConnectSecurityError;
import org.nuxeo.connect.data.SubscriptionStatus;

/**
 * Simple Wrapper to add label computation to the {@link SubscriptionStatus}
 *
 * @author tiry
 */
public class SubscriptionStatusWrapper extends SubscriptionStatus {

    protected boolean isSecurityError=false;

    protected boolean canNotReachConnectServer = false;

    protected boolean versionMismatch = false;

    protected Calendar refreshDate;

    public SubscriptionStatusWrapper(String errorMessage) {
        this.errorMessage = errorMessage;
        refreshDate = Calendar.getInstance();
    }

    public SubscriptionStatusWrapper(ConnectSecurityError securityException) {
        this(securityException.getMessage());
        isSecurityError = true;
    }

    public SubscriptionStatusWrapper(SubscriptionStatus status) {
        contractStatus = status.getContractStatus();
        description = status.getDescription();
        instanceType = status.getInstanceType();
        message = status.getMessage();
        errorMessage = status.getErrorMessage();
        endDate = status.getEndDate();
        refreshDate = Calendar.getInstance();
    }

    public String getinstanceTypeLabel() {
        return "label.instancetype." + getInstanceType().toString();
    }

    public String getContractStatusLabel() {
        return "label.subscriptionStatus." + getContractStatus();
    }

    public boolean isError() {
        return errorMessage!=null || isSecurityError || versionMismatch || canNotReachConnectServer;
    }

    public boolean isConnectServerUnreachable() {
        return canNotReachConnectServer;
    }

    public boolean isVersionMismatch() {
        return versionMismatch;
    }

    public boolean isSecurityError() {
        return isSecurityError;
    }

    public Calendar getRefreshDate() {
        return refreshDate;
    }

}
