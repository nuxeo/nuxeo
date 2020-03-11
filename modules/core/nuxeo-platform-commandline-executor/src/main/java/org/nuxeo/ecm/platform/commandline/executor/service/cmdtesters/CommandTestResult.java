/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 *
 */

package org.nuxeo.ecm.platform.commandline.executor.service.cmdtesters;

import java.io.Serializable;

/**
 * Wraps result for a CommandTest execution.
 *
 * @author tiry
 */
public class CommandTestResult implements Serializable {

    private static final long serialVersionUID = 1L;

    protected final boolean testSuccess;

    protected final String errorMessage;

    public CommandTestResult() {
        testSuccess = true;
        errorMessage = null;
    }

    public CommandTestResult(String error) {
        errorMessage = error;
        testSuccess = false;
    }

    public boolean succeed() {
        return testSuccess;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

}
