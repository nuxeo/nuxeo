/*
 * Copyright (c) 2006-2013 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     vpasquier
 *     slacoin
 */
package org.nuxeo.ecm.automation;

import java.util.LinkedList;
import java.util.List;

/**
 * @since 5.7.2 Operation composite exception builder throwing @{link
 *        OperationCompoundException}.
 */
public class OperationCompoundExceptionBuilder {

    protected final List<OperationException> accumulated = new LinkedList<OperationException>();

    protected OperationException newThrowable(List<OperationException> causes) {
        return new OperationCompoundException(getMessages(causes),
                causes.toArray(new OperationException[causes.size()]));
    }

    protected String getMessages(List<OperationException> causes) {
        StringBuilder stringBuilder = new StringBuilder();
        for (OperationException operationException : causes) {
            stringBuilder.append(operationException.getMessage());
            stringBuilder.append(System.getProperty("line.separator"));
        }
        return stringBuilder.toString();
    }

    public void add(OperationException error) {
        accumulated.add(error);
    }

    public void throwOnError() throws OperationException {
        if (accumulated.isEmpty()) {
            return;
        }
        throw newThrowable(accumulated);
    }

}
