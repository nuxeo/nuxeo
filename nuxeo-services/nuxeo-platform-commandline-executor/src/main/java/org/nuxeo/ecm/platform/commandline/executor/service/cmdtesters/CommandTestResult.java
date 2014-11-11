/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

    protected final boolean testSucces;

    protected String errorMessage;

    public CommandTestResult() {
        testSucces = true;
    }

    public CommandTestResult(String error) {
        errorMessage = error;
        testSucces = false;
    }

    public boolean succeed() {
        return testSucces;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

}
