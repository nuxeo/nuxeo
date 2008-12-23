/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.directory.ldap;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.directory.DirectoryException;


@XObject(value = "server")
public class LDAPServerDescriptor {

    @XNode("@name")
    public String name;

    public String ldapUrls;

    public String bindDn;

    @XNode("poolingEnabled")
    public boolean poolingEnabled = true;

    public String getName() {
        return name;
    }

    public String bindPassword = "";

    @XNode("bindDn")
    public void setBindDn(String bindDn) {
        if (null != bindDn && bindDn.trim().equals("")) {
            // empty bindDn means anonymous authentication
            this.bindDn = null;
        } else {
            this.bindDn = bindDn;
        }
    }

    public String getBindDn() {
        return bindDn;
    }

    @XNode("bindPassword")
    public void setBindPassword(String bindPassword) {
        if (bindPassword == null) {
            // no password means empty pasword
            this.bindPassword = "";
        } else {
            this.bindPassword = bindPassword;
        }
    }

    public String getBindPassword() {
        return bindPassword;
    }

    public String getLdapUrls() {
        return ldapUrls;
    }

    @XNodeList(value = "ldapUrl", componentType = String.class, type = String[].class)
    public void setLdapUrls(String[] ldapUrls) throws DirectoryException {
        if (ldapUrls == null) {
            throw new DirectoryException(
                    "At least one <ldapUrl/> server declaration is required");
        }
        // Leverage JNDI support for clustered servers by concatinating
        // all the provided URLs for failover
        this.ldapUrls = StringUtils.join(ldapUrls, " ");
    }

    public boolean isPoolingEnabled() {
        return poolingEnabled;
    }

}
