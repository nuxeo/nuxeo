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
