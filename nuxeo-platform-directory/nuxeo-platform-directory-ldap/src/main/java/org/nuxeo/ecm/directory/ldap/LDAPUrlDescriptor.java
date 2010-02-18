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
 *     Robert Browning - initial implementation
 *     Nuxeo - code review and integration
 */
package org.nuxeo.ecm.directory.ldap;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.directory.ldap.dns.DNSServiceResolver;

@XObject
public class LDAPUrlDescriptor {

    @XNode(value = "@srvPrefix")
    private String srvPrefix = DNSServiceResolver.LDAP_SERVICE_PREFIX;

    @XNode
    private String value;

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof LDAPUrlDescriptor) {
            return value.equals(((LDAPUrlDescriptor) obj).value)
                    && srvPrefix.equals(((LDAPUrlDescriptor) obj).srvPrefix);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return (srvPrefix + value).hashCode();
    }

    public String getSrvPrefix() {
        return srvPrefix;
    }

    public String getValue() {
        return value;
    }

    public void setSrvPrefix(String srvPrefix) {
        this.srvPrefix = srvPrefix;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
