/*
 *
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

package org.nuxeo.ecm.platform.web.common.session;

import java.util.Date;

/**
 * Stores informations about a user's Http Session
 *
 * @author Tiry (tdelprat@nuxeo.com)
 * @since 5.4.2
 */
public class SessionInfo implements Comparable<SessionInfo> {

    protected final String sessionId;

    protected final long creationTime;

    protected long lastAccessTime;

    protected int nbAccess = 0;

    protected String lastAccessUrl;

    protected String loginName;

    public SessionInfo(String sid) {
        creationTime = System.currentTimeMillis();
        lastAccessTime = creationTime;
        sessionId = sid;
    }

    public long getLastAccessTime() {
        return lastAccessTime;
    }

    public void updateLastAccessTime() {
        lastAccessTime = System.currentTimeMillis();
    }

    public String getLastAccessUrl() {
        return lastAccessUrl;
    }

    public void setLastAccessUrl(String lastAccessUrl) {
        this.lastAccessUrl = lastAccessUrl;
        nbAccess += 1;
    }

    public long getCreationTime() {
        return creationTime;
    }

    public String getLoginName() {
        return loginName;
    }

    public void setLoginName(String loginName) {
        this.loginName = loginName;
    }

    public String getSessionId() {
        return sessionId;
    }

    public long getDurationInS() {
        return (System.currentTimeMillis() - creationTime) / 1000;
    }

    public long getInactivityInS() {
        return (System.currentTimeMillis() - lastAccessTime) / 1000;
    }

    protected String formatDuration(long nbs) {
        StringBuffer sb = new StringBuffer();

        long nbh = nbs / 3600;
        nbs = nbs - nbh * 3600;
        long nbm = nbs / 60;
        nbs = nbs - nbm * 60;

        if (nbh > 0) {
            sb.append(nbh);
            sb.append("h ");
        }
        if (nbm > 0 || nbh > 0) {
            sb.append(nbm);
            sb.append("m ");
        }

        sb.append(nbs);
        sb.append("s ");

        return sb.toString();
    }

    public String getDurationAsString() {
        return formatDuration(getDurationInS());
    }

    public String getInactivityAsString() {
        return formatDuration(getInactivityInS());
    }

    public Date getLastAccessDate() {
        return new Date(getLastAccessTime());
    }

    @Override
    public int compareTo(SessionInfo o) {
        if (getInactivityInS()==o.getInactivityInS()) {
            return 0;
        }
        return getInactivityInS() > o.getInactivityInS() ? 1 : -1;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Sid=");
        sb.append(sessionId);
        sb.append(" : login=");
        sb.append(loginName);
        return sb.toString();
    }

    public int getAccessedPagesCount() {
        return nbAccess;
    }
}
