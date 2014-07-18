/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Julien Carsique
 *
 */

package org.nuxeo.runtime.test;

import java.util.List;

import junit.framework.Assert;
import junit.framework.AssertionFailedError;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;

/**
 * Utility class for working with {@link org.junit.runner.Result#getFailures()}
 *
 * @since 5.9.5
 */
public class Failures {
    private static final Log log = LogFactory.getLog(Failures.class);

    private List<Failure> failures;

    public Failures(List<Failure> failures) {
        this.failures = failures;
    }

    public Failures(Result result) {
        this.failures = result.getFailures();
    }

    @Override
    public String toString() {
        StringBuffer buffer = new StringBuffer();
        int i = 1;
        AssertionFailedError errors = new AssertionFailedError();
        for (Failure failure : failures) {
            buffer.append("* Failure " + i + ": ") //
            .append(failure.getTestHeader()).append("\n") //
            .append(failure.getTrace()).append("\n");
            errors.addSuppressed(failure.getException());
            i++;
        }
        if (errors.getSuppressed().length > 0) {
            // Log because JUnit swallows some parts of the stack trace
            log.debug(errors.getMessage(), errors);
        }
        return buffer.toString();
    }

    /**
     * Call {@link org.junit.Assert#fail(String)} with a nice expanded string if
     * there are failures. It also replaces original failure messages with a
     * custom one if originalMessage is not {@code null}.
     *
     * @param originalMessage Message to replace if found in a failure
     * @param customMessage Custom message to use as replacement for
     *            originalMessage
     */
    public void fail(String originalMessage, String customMessage) {
        if (failures.isEmpty()) {
            return;
        }
        StringBuffer buffer = new StringBuffer();
        int i = 1;
        AssertionFailedError errors = new AssertionFailedError();
        for (Failure failure : failures) {
            buffer.append("\n* Failure " + i + ": ");
            if (originalMessage != null
                    && originalMessage.equals(failure.getMessage())) {
                buffer.append(failure.getTestHeader() + ":" + customMessage);
            } else {
                buffer.append(String.format("Unexpected failure at %s\n%s",
                        failure.getTestHeader(), failure.getTrace()));
                errors.addSuppressed(failure.getException());
            }
            i++;
        }
        // Log because JUnit swallows some parts of the stack trace
        log.error(errors.getMessage(), errors);
        Assert.fail(buffer.toString());
    }
}
