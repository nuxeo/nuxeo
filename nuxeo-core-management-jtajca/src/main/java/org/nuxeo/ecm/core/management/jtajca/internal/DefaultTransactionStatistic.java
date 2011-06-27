/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     matic
 */
package org.nuxeo.ecm.core.management.jtajca.internal;

import java.text.DateFormat;
import java.util.Date;

import org.javasimon.Split;
import org.nuxeo.ecm.core.management.jtajca.TransactionStatistics;

/**
 * @author matic
 * 
 */
public class DefaultTransactionStatistic implements TransactionStatistics {

    protected final Object key;

    protected long startTimestamp;

    protected String threadName;

    protected long endTimestamp;

    protected String endStatus;

    protected Split split;

    protected DefaultTransactionStatistic(Object key) {
        this.key = key;
    }

    @Override
    public Date getStartDate() {
        return new Date(startTimestamp);
    }

    @Override
    public Date getEndDate() {
        return new Date(endTimestamp);
    }

    @Override
    public long getDuration() {
        return endTimestamp - startTimestamp;
    }

    @Override
    public String getId() {
        return key.toString();
    }

    @Override
    public String getThreadName() {
        return threadName;
    }

    @Override
    public boolean isEnded() {
        return endTimestamp != 0;
    }

    @Override
    public String toString() {
        final String date = DateFormat.getDateInstance(DateFormat.MEDIUM).format(
                new Date(startTimestamp));
        final long duration = endTimestamp - startTimestamp;
        if (endTimestamp == 0) {
            return String.format(
                    "Transaction %s, started at %s, has a duration of %d ms",
                    key.toString(), date, duration, endStatus);
        }
        return String.format(
                "Transaction %s, started at %s, with a duration of %d ms, has %s",
                key.toString(), date, duration, endStatus);
    }
}
