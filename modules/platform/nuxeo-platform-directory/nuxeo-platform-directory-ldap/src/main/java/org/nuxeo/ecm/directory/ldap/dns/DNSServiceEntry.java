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
package org.nuxeo.ecm.directory.ldap.dns;

/**
 * Encapsulates a hostname and port.
 */
public class DNSServiceEntry implements Comparable<DNSServiceEntry> {

    private final String host;

    private final int port;

    private final int priority;

    private final int weight;

    public DNSServiceEntry(String host, int port, int priority, int weight) {
        this.host = host;
        this.port = port;
        this.priority = priority;
        this.weight = weight;
    }

    /**
     * Get the priority of this DNS entry, descending priority 0(highest)..Integer.MAX_VALUE(lowest)
     */
    public int getPriority() {
        return priority;
    }

    /**
     * Get the weight of this DNS entry to compare entries with equal priority, ascending weight
     * 0(lowest)..Integer.MAX_VALUE(highest)
     */
    public int getWeight() {
        return weight;
    }

    /**
     * Returns the hostname.
     */
    public String getHost() {
        return host;
    }

    /**
     * Returns the port.
     */
    public int getPort() {
        return port;
    }

    /**
     * Return the hostname in the form (hostname:port)
     */
    @Override
    public String toString() {
        return host + ":" + port;
    }

    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DNSServiceEntry)) {
            return false;
        }

        final DNSServiceEntry address = (DNSServiceEntry) o;

        if (!host.equals(address.host)) {
            return false;
        }
        return port == address.port;
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    /**
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(DNSServiceEntry o) {
        if (o.priority == priority) {
            return o.weight - weight;
        }
        return priority - o.priority;
    }
}
