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
 *     mcedica
 */
package org.nuxeo.ecm.platform.management.statuses;

public class ProbeStatus {

    protected final  boolean success;

    protected final String info;

    protected ProbeStatus(String info, Boolean success){
        this.info = info;
        this.success = success;
    }

    public static ProbeStatus newFailure(String info) {
        return new ProbeStatus(info, false);
    }

    public static ProbeStatus newError(Throwable t) {
        return new ProbeStatus("Caught error " + t, false);
    }

    public static ProbeStatus newSuccess(String info) {
        return new ProbeStatus(info, true);
    }

    public String getInfo() {
        return info;
    }

    public boolean isSuccess() {
        return success;
    }

    public boolean isFailure() {
        return !success;
    }

}

