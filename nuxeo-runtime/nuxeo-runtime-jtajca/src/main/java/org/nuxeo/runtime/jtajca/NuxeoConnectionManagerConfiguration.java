/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 *     Julien Carsique
 */
package org.nuxeo.runtime.jtajca;

import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.runtime.jtajca.NuxeoValidationSupport.Validation;

/**
 * Descriptor of the pool configuration, used by NuxeoContainer when creating a pool directly instead of the previous
 * way that was using a JNDI factory (NuxeoConnectionManagerFactory).
 *
 * @since 5.6
 */
@XObject("pool")
public class NuxeoConnectionManagerConfiguration {

    public static final int DEFAULT_MAX_POOL_SIZE = 20;

    public static final int DEFAULT_MIN_POOL_SIZE = 0;

    public static final int DEFAULT_BLOCKING_TIMEOUT_MILLIS = 100;

    public static final int DEFAULT_IDLE_TIMEOUT_MINUTES = 0; // no timeout

    public static final int DEFAULT_ACTIVE_TIMEOUT_MINUTES = 0; // no timeout

    @XNode("@name")
    private String name = "NuxeoConnectionManager";

    // transaction
    @XNode("@xaMode")
    private Boolean xaMode;

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

    @XNode("@maxPoolSize")
    private Integer maxPoolSize;

    @XNode("@minPoolSize")
    private Integer minPoolSize;

    @XNode("@blockingTimeoutMillis")
    private Integer blockingTimeoutMillis;

    @XNode("@idleTimeoutMinutes")
    private Integer idleTimeoutMinutes;

    @XNode("@activeTimeoutMinutes")
    private Integer activeTimeoutMinutes;

    Validation testOnBorrow;

    Validation testOnReturn;

    public NuxeoConnectionManagerConfiguration() {
    }

    /** Copy constructor. */
    public NuxeoConnectionManagerConfiguration(NuxeoConnectionManagerConfiguration other) {
        name = other.name;
        useTransactionCaching = other.useTransactionCaching;
        useThreadCaching = other.useThreadCaching;
        matchOne = other.matchOne;
        matchAll = other.matchAll;
        selectOneNoMatch = other.selectOneNoMatch;
        maxPoolSize = other.maxPoolSize;
        minPoolSize = other.minPoolSize;
        blockingTimeoutMillis = other.blockingTimeoutMillis;
        idleTimeoutMinutes = other.idleTimeoutMinutes;
        activeTimeoutMinutes = other.activeTimeoutMinutes;
        testOnBorrow = other.testOnBorrow;
        testOnReturn = other.testOnReturn;
    }

    public void merge(NuxeoConnectionManagerConfiguration other) {
        if (other.name != null) {
            name = other.name;
        }
        if (other.xaMode) {
            xaMode = other.xaMode;
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
        if (other.activeTimeoutMinutes != null) {
            activeTimeoutMinutes = other.activeTimeoutMinutes;
        }
        if (other.testOnBorrow != null) {
            testOnBorrow = other.testOnBorrow;
        }
        if (other.testOnReturn != null) {
            testOnReturn = other.testOnReturn;
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

    public boolean getXAMode() {
        return defaultTrue(xaMode);
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

    public int getMaxPoolSize() {
        return defaultInt(maxPoolSize, DEFAULT_MAX_POOL_SIZE);
    }

    public int getMinPoolSize() {
        return defaultInt(minPoolSize, DEFAULT_MIN_POOL_SIZE);
    }

    public int getBlockingTimeoutMillis() {
        return defaultInt(blockingTimeoutMillis, DEFAULT_BLOCKING_TIMEOUT_MILLIS);
    }

    public int getIdleTimeoutMinutes() {
        return defaultInt(idleTimeoutMinutes, DEFAULT_IDLE_TIMEOUT_MINUTES);
    }

    public int getActiveTimeoutMinutes() {
        return defaultInt(activeTimeoutMinutes, DEFAULT_ACTIVE_TIMEOUT_MINUTES);
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setXAMode(boolean xaMode) {
        this.xaMode = Boolean.valueOf(xaMode);
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

    public void setActiveTimeoutMinutes(int activeTimeoutMinutes) {
        this.activeTimeoutMinutes = Integer.valueOf(activeTimeoutMinutes);
    }

    @XNode("@validationQuery")
    public void setValidationQuery(String sql) {
        if (sql.isEmpty()) {
            testOnBorrow = null;
        } else {
            testOnBorrow = new NuxeoValidationSupport.QuerySQLConnection(sql);
        }
    }


    @XNode("@testOnBorrow")
    public void setTestOnBorrow(Class<? extends Validation> typeof) throws ReflectiveOperationException {
        testOnBorrow = typeof.newInstance();
    }

    @XNode("@testOnReturn")
    public void setTestOnReturn(Class<? extends Validation> typeof) throws ReflectiveOperationException {
        testOnReturn = typeof.newInstance();
    }

    @XNode("@maxActive")
    public void setMaxActive(int num) {
        maxPoolSize = num;
        LogFactory.getLog(NuxeoConnectionManagerConfiguration.class).warn(
                "maxActive deprecated dbcp pool attribute usage, should use maxPoolSize geronimo pool attribute instead");
    }

    @XNode("@maxIdle")
    public void setMaxIdle(int num) {
        minPoolSize = num;
        LogFactory.getLog(NuxeoConnectionManagerConfiguration.class).warn(
                "maxIdle deprecated dbcp pool attribute usage, should use minPoolSize geronimo pool attribute instead");
    }

    @XNode("@maxWait")
    public void setMaxWait(int num) {
        blockingTimeoutMillis = num;
        LogFactory.getLog(NuxeoConnectionManagerConfiguration.class).warn(
                "maxWait deprecated dbcp pool attribute usage, should use blockingTimeoutMillis geronimo pool attribute instead");

    }


}
