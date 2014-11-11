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
 *     matic
 */
package org.nuxeo.ecm.core.management.api;

import java.util.Date;

/**
 * @author Stephane Lacoin (Nuxeo EP Software Engineer)
 */
public interface ProbeMBean {

    boolean isEnabled();

    void enable();

    void disable();

    boolean isInError();

    long getRunnedCount();

    Date getLastRunnedDate();

    long getLastDuration();

    long getSucceedCount();

    Date getLastSucceedDate();

    long getFailedCount();

    Date getLastFailedDate();

    ProbeStatus getLastFailureStatus();

}
