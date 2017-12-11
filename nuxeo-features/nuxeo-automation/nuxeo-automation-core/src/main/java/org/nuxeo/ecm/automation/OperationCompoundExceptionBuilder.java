/*
 * (C) Copyright 2006-2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     vpasquier
 *     slacoin
 */
package org.nuxeo.ecm.automation;

import java.util.LinkedList;
import java.util.List;

/**
 * @since 5.7.2 Operation composite exception builder throwing @{link OperationCompoundException}.
 * @deprecated since 9.10. Not used anymore.
 */
@Deprecated
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
