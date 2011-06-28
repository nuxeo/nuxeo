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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DateFormat;
import java.util.Date;

import org.javasimon.Split;
import org.nuxeo.ecm.core.management.jtajca.TransactionStatistics;

/**
 * @author matic
 * 
 */
public class DefaultTransactionStatistics implements TransactionStatistics {

    protected final Object id;

    protected long startTimestamp;

    protected Throwable startCapturedContext;

    protected  String threadName;

    protected long endTimestamp;

    protected Throwable endCapturedContext;

    protected Status status;

    protected Split split;

    protected DefaultTransactionStatistics(Object k) {
        this.id = k;
    }
    
    @Override
    public String getId() {
        return id.toString();
    }
        
    
    @Override
    public String getThreadName() {
        return threadName;
    }
    
    public Status getStatus() {
        return status;
    }
    
    @Override
    public Date getStartDate() {
        return new Date(startTimestamp);
    }

    public Throwable getStartCapturedContext() {
        return startCapturedContext;
    }

    @Override
    public String getStartCapturedContextMessage() {
        return printCapturedContext(startCapturedContext);
    }
        
    @Override
    public Date getEndDate() {
        return new Date(endTimestamp);
    }

    public Throwable getEndCapturedContext() {
        return endCapturedContext;
    }

    @Override
    public String getEndCapturedContextMessage() {
        if (endCapturedContext == null) {
            return "no context";
        }
        return printCapturedContext(endCapturedContext);
    }
    
    @Override
    public long getDuration() {
        return endTimestamp - startTimestamp;
    }

    @Override
    public boolean isEnded() {
        return endTimestamp != 0;
    }

    protected static String printCapturedContext(Throwable e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw, false);
        e.printStackTrace(pw);
        pw.flush();
        return sw.toString();
    }
    
    @Override
    public String toString() {
        final String date = DateFormat.getDateInstance(DateFormat.MEDIUM).format(
                new Date(startTimestamp));
        final long duration = endTimestamp - startTimestamp;
        if (endTimestamp == 0) {
            return String.format(
                    "Transaction %s, has started at %s with a duration of %d ms and has %s\n%s%s",
                    id.toString(), date, duration, status, getStartCapturedContextMessage(), getEndCapturedContextMessage());
        }
        return String.format(
                "Transaction %s has started at %s and is still active after %d ms\n%s",
                id.toString(), date, duration, getStartCapturedContextMessage());
    }
}
