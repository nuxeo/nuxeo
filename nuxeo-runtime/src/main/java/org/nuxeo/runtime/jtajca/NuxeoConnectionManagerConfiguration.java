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

    public static final int DEFAULT_MAX_POOL_SIZE = 20;

    public static final int DEFAULT_MIN_POOL_SIZE = 0;

    public static final int DEFAULT_BLOCKING_TIMEOUT_MILLIS = 100;

    public static final int DEFAULT_IDLE_TIMEOUT_MINUTES = 0; // no timeout

    @XNode("@name")
    private String name = "NuxeoConnectionManager";

    // transaction

    @XNode("@useTransactionCaching")
    private Boolean useTransactionCaching;

    @XNode("@useThreadCaching")
    private Boolean useThreadCaching;

    // pool

    @XNode("@matchOne")
    private Boolean matchOne; // unused by Geronimo?

    @XNode("@matchAll")
    private Boolean matchAll;

    @XNode("@selectOneNoMatch")
    private Boolean selectOneNoMatch;

    @XNode("@partitionByConnectionRequestInfo")
    private Boolean partitionByConnectionRequestInfo;

    @XNode("@partitionBySubject")
    private Boolean partitionBySubject;

    @XNode("@maxPoolSize")
    private Integer maxPoolSize;

    @XNode("@minPoolSize")
    private Integer minPoolSize;

    @XNode("@blockingTimeoutMillis")
    private Integer blockingTimeoutMillis;

    @XNode("@idleTimeoutMinutes")
    private Integer idleTimeoutMinutes;

    public NuxeoConnectionManagerConfiguration() {
    }

    /** Copy constructor. */
    public NuxeoConnectionManagerConfiguration(
            NuxeoConnectionManagerConfiguration other) {
        name = other.name;
        useTransactionCaching = other.useTransactionCaching;
        useThreadCaching = other.useThreadCaching;
        matchOne = other.matchOne;
        matchAll = other.matchAll;
        selectOneNoMatch = other.selectOneNoMatch;
        partitionByConnectionRequestInfo = other.partitionByConnectionRequestInfo;
        partitionBySubject = other.partitionBySubject;
        maxPoolSize = other.maxPoolSize;
        minPoolSize = other.minPoolSize;
        blockingTimeoutMillis = other.blockingTimeoutMillis;
        idleTimeoutMinutes = other.idleTimeoutMinutes;
    }

    public void merge(NuxeoConnectionManagerConfiguration other) {
        if (other.name != null) {
            name = other.name;
        }
        if (other.useTransactionCaching != null) {
            useTransactionCaching = other.useTransactionCaching;
        }
        if (other.useThreadCaching != null) {
            useThreadCaching = other.useThreadCaching;
        }
        if (other.matchOne != null) {
            matchOne = other.matchOne;
        }
        if (other.matchAll != null) {
            matchAll = other.matchAll;
        }
        if (other.selectOneNoMatch != null) {
            selectOneNoMatch = other.selectOneNoMatch;
        }
        if (other.partitionByConnectionRequestInfo != null) {
            partitionByConnectionRequestInfo = other.partitionByConnectionRequestInfo;
        }
        if (other.partitionBySubject != null) {
            partitionBySubject = other.partitionBySubject;
        }
        if (other.maxPoolSize != null) {
            maxPoolSize = other.maxPoolSize;
        }
        if (other.minPoolSize != null) {
            minPoolSize = other.minPoolSize;
        }
        if (other.blockingTimeoutMillis != null) {
            blockingTimeoutMillis = other.blockingTimeoutMillis;
        }
        if (other.idleTimeoutMinutes != null) {
            idleTimeoutMinutes = other.idleTimeoutMinutes;
        }
    }

    /** False if the boolean is null or FALSE, true otherwise. */
    private static boolean defaultFalse(Boolean bool) {
        return Boolean.TRUE.equals(bool);
    }

    /** True if the boolean is null or TRUE, false otherwise. */
    private static boolean defaultTrue(Boolean bool) {
        return !Boolean.FALSE.equals(bool);
    }

    private static int defaultInt(Integer value, int def) {
        return value == null ? def : value.intValue();
    }

    public String getName() {
        return name;
    }

    public boolean getUseTransactionCaching() {
        return defaultTrue(useTransactionCaching);
    }

    public boolean getUseThreadCaching() {
        return defaultTrue(useThreadCaching);
    }

    public boolean getMatchOne() {
        return defaultTrue(matchOne);
    }

    public boolean getMatchAll() {
        return defaultTrue(matchAll);
    }

    public boolean getSelectOneNoMatch() {
        return defaultFalse(selectOneNoMatch);
    }

    public boolean getPartitionByConnectionRequestInfo() {
        return defaultFalse(partitionByConnectionRequestInfo);
    }

    public boolean getPartitionBySubject() {
        return defaultTrue(partitionBySubject);
    }

    public int getMaxPoolSize() {
        return defaultInt(maxPoolSize, DEFAULT_MAX_POOL_SIZE);
    }

    public int getMinPoolSize() {
        return defaultInt(minPoolSize, DEFAULT_MIN_POOL_SIZE);
    }

    public int getBlockingTimeoutMillis() {
        return defaultInt(blockingTimeoutMillis,
                DEFAULT_BLOCKING_TIMEOUT_MILLIS);
    }

    public int getIdleTimeoutMinutes() {
        return defaultInt(idleTimeoutMinutes, DEFAULT_IDLE_TIMEOUT_MINUTES);
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setUseTransactionCaching(boolean useTransactionCaching) {
        this.useTransactionCaching = Boolean.valueOf(useTransactionCaching);
    }

    public void setUseThreadCaching(boolean useThreadCaching) {
        this.useThreadCaching = Boolean.valueOf(useThreadCaching);
    }

    public void setMatchOne(boolean matchOne) {
        this.matchOne = Boolean.valueOf(matchOne);
    }

    public void setMatchAll(boolean matchAll) {
        this.matchAll = Boolean.valueOf(matchAll);
    }

    public void setSelectOneNoMatch(boolean selectOneNoMatch) {
        this.selectOneNoMatch = Boolean.valueOf(selectOneNoMatch);
    }

    public void setPartitionByConnectionRequestInfo(
            boolean partitionByConnectionRequestInfo) {
        this.partitionByConnectionRequestInfo = Boolean.valueOf(partitionByConnectionRequestInfo);
    }

    public void setPartitionBySubject(boolean partitionBySubject) {
        this.partitionBySubject = Boolean.valueOf(partitionBySubject);
    }

    public void setMaxPoolSize(int maxPoolSize) {
        this.maxPoolSize = Integer.valueOf(maxPoolSize);
    }

    public void setMinPoolSize(int minPoolSize) {
        this.minPoolSize = Integer.valueOf(minPoolSize);
    }

    public void setBlockingTimeoutMillis(int blockingTimeoutMillis) {
        this.blockingTimeoutMillis = Integer.valueOf(blockingTimeoutMillis);
    }

    public void setIdleTimeoutMinutes(int idleTimeoutMinutes) {
        this.idleTimeoutMinutes = Integer.valueOf(idleTimeoutMinutes);
    }

}
