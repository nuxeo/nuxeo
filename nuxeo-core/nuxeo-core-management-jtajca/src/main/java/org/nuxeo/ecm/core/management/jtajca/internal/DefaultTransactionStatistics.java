/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
public class DefaultTransactionStatistics implements TransactionStatistics {

    protected final String id;

    protected long startTimestamp;

    protected Throwable startCapturedContext;

    protected String threadName;

    protected long endTimestamp;

    protected Throwable endCapturedContext;

    protected Status status;

    protected Split split;

    protected DefaultTransactionStatistics(Object key) {
        id = DefaultTransactionMonitor.id(key);
    }

    @Override
    public String getId() {
        return id.toString();
    }

    @Override
    public String getThreadName() {
        return threadName;
    }

    @Override
    public Status getStatus() {
        return status;
    }

    @Override
    public Date getStartDate() {
        return new Date(startTimestamp);
    }

    public String getStartCapturedContext() {
        return printCapturedContext(startCapturedContext);
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

    public void print(PrintWriter writer) {
        final String date = DateFormat.getDateInstance(DateFormat.MEDIUM).format(new Date(startTimestamp));
        final long duration = getDuration();
        if (duration > 0) {
            writer.write(String.format(
                    "Transaction has started at %s with a duration of %d ms and was in status %s\n%s", date, duration,
                    status, getEndCapturedContextMessage()));
        } else {
            writer.write(String.format("Transaction has started at %s and was in status %s\n%s", date, status,
                    getStartCapturedContextMessage()));
        }
        writer.flush();
    }

    @Override
    public String toString() {
        StringWriter sw = new StringWriter();
        print(new PrintWriter(sw));
        return sw.toString();
    }
}
