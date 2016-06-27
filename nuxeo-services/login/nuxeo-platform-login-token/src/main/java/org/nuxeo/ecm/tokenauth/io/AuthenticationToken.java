/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nelson Silva <nsilva@nuxeo.com>
 */
package org.nuxeo.ecm.tokenauth.io;

import java.util.Calendar;

/**
 * @since 8.3
 */
public class AuthenticationToken {

    protected String token;

    protected String userName;

    protected String applicationName;

    protected String deviceId;

    protected String deviceDescription;

    protected String permission;

    private Calendar creationDate;

    public AuthenticationToken(String token, String userName, String applicationName, String deviceId,
            String deviceDescription, String permission) {
        this.token = token;
        this.userName = userName;
        this.applicationName = applicationName;
        this.deviceId = deviceId;
        this.deviceDescription = deviceDescription;
        this.permission = permission;
    }

    public String getToken() {
        return token;
    }

    public String getUserName() {
        return userName;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public String getDeviceDescription() {
        return deviceDescription;
    }

    public String getPermission() {
        return permission;
    }

    public Calendar getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Calendar creationDate) {
        this.creationDate = creationDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        AuthenticationToken that = (AuthenticationToken) o;

        if (!getToken().equals(that.getToken()))
            return false;
        if (!getUserName().equals(that.getUserName()))
            return false;
        if (!getApplicationName().equals(that.getApplicationName()))
            return false;
        if (!getDeviceId().equals(that.getDeviceId()))
            return false;
        if (getDeviceDescription() != null ?
                !getDeviceDescription().equals(that.getDeviceDescription()) :
                that.getDeviceDescription() != null)
            return false;
        return getPermission().equals(that.getPermission());
    }

    @Override
    public int hashCode() {
        int result = getToken().hashCode();
        result = 31 * result + getUserName().hashCode();
        result = 31 * result + getApplicationName().hashCode();
        result = 31 * result + getDeviceId().hashCode();
        result = 31 * result + getPermission().hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "AuthenticationToken{" +
                "token='" + token + '\'' +
                ", userName='" + userName + '\'' +
                ", applicationName='" + applicationName + '\'' +
                ", deviceId='" + deviceId + '\'' +
                ", deviceDescription='" + deviceDescription + '\'' +
                ", permission='" + permission + '\'' +
                ", creationDate=" + creationDate +
                '}';
    }
}
