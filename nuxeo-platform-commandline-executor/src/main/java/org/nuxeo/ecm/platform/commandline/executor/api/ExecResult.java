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

package org.nuxeo.ecm.platform.commandline.executor.api;

import java.io.Serializable;
import java.util.List;

/**
 *
 * Wraps result of the commandline execution :
 *  - output buffer
 *  - return code
 *  - java Exception
 *
 * @author tiry
 *
 */
public class ExecResult implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    protected List<String> output;

    protected long execTime;

    protected boolean success;

    protected Exception error;

    protected int returnCode;

    public ExecResult(List<String> output, long execTime, int returnCode) {
        this.execTime = execTime;
        this.output = output;
        this.success=true;
        this.error=null;
        this.returnCode=returnCode;
        if (returnCode != 0) {
            this.success=false;
        }
    }

    public ExecResult( Exception error) {
        this.execTime = 0;
        this.output = null;
        this.success=false;
        this.error=error;
    }


    public List<String> getOutput() {
        return output;
    }

    public long getExecTime() {
        return execTime;
    }

    public boolean isSuccessful() {
        return success;
    }

    public Exception getError() {
        return error;
    }

    public int getReturnCode() {
        return returnCode;
    }
}
