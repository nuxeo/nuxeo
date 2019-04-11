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

    protected boolean isSecurityError = false;

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

    @Override
    public boolean isError() {
        return errorMessage != null || isSecurityError || versionMismatch || canNotReachConnectServer;
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
