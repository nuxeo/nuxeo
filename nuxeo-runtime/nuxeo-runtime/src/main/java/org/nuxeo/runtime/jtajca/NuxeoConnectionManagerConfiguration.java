/*
 * Copyright (c) 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Florent Guillaume
 *     Julien Carsique
 */
package org.nuxeo.runtime.jtajca;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * Descriptor of the pool configuration, used by NuxeoContainer when creating a
 * pool directly instead of the previous way that was using a JNDI factory
 * (NuxeoConnectionManagerFactory).
 *
 * @since 5.6
 */
@XObject("pool")
public class NuxeoConnectionManagerConfiguration {

    public String name = "NuxeoConnectionManager";

    // transaction

    public boolean useTransactionCaching = true;

    public boolean useThreadCaching = true;

    // pool

    public boolean matchOne = true; // unused by Geronimo?

    public boolean matchAll = true;

    public boolean selectOneNoMatch = false;

    public boolean partitionByConnectionRequestInfo = false;

    public boolean partitionBySubject = true;

    public int maxPoolSize = 20;

    public int minPoolSize = 0;

    public int blockingTimeoutMillis = 100;

    public int idleTimeoutMinutes = 0; // no timeout

    /*
     * Setters used for Bean API to set values from
     * NuxeoConnectionManagerFactory.
     */

    @XNode("@name")
    public void setName(String name) {
        this.name = name;
    }

    @XNode("@useTransactionCaching")
    public void setUseTransactionCaching(boolean useTransactionCaching) {
        this.useTransactionCaching = useTransactionCaching;
    }

    @XNode("@useThreadCaching")
    public void setUseThreadCaching(boolean useThreadCaching) {
        this.useThreadCaching = useThreadCaching;
    }

    @XNode("@matchOne")
    public void setMatchOne(boolean matchOne) {
        this.matchOne = matchOne;
    }

    @XNode("@matchAll")
    public void setMatchAll(boolean matchAll) {
        this.matchAll = matchAll;
    }

    @XNode("@selectOneNoMatch")
    public void setSelectOneNoMatch(boolean selectOneNoMatch) {
        this.selectOneNoMatch = selectOneNoMatch;
    }

    @XNode("@partitionByConnectionRequestInfo")
    public void setPartitionByConnectionRequestInfo(
            boolean partitionByConnectionRequestInfo) {
        this.partitionByConnectionRequestInfo = partitionByConnectionRequestInfo;
    }

    @XNode("@partitionBySubject")
    public void setPartitionBySubject(boolean partitionBySubject) {
        this.partitionBySubject = partitionBySubject;
    }

    @XNode("@maxPoolSize")
    public void setMaxPoolSize(int maxPoolSize) {
        this.maxPoolSize = maxPoolSize;
    }

    @XNode("@minPoolSize")
    public void setMinPoolSize(int minPoolSize) {
        this.minPoolSize = minPoolSize;
    }

    @XNode("@blockingTimeoutMillis")
    public void setBlockingTimeoutMillis(int blockingTimeoutMillis) {
        this.blockingTimeoutMillis = blockingTimeoutMillis;
    }

    @XNode("@idleTimeoutMinutes")
    public void setIdleTimeoutMinutes(int idleTimeoutMinutes) {
        this.idleTimeoutMinutes = idleTimeoutMinutes;
    }

}
