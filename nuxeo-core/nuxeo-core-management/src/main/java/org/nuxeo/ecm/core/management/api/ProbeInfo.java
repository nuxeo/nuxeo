/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.core.management.api;

import java.util.Date;

import org.nuxeo.ecm.core.management.probes.ProbeDescriptor;

public interface ProbeInfo {

    long getFailedCount();

    long getLastDuration();

    Date getLastFailedDate();

    Date getLastRunnedDate();

    Date getLastSucceedDate();

    long getRunnedCount();

    long getSucceedCount();

    boolean isEnabled();

    boolean isInError();

    ProbeStatus getStatus();

    String getShortcutName();

    String getQualifiedName();

    ProbeStatus getLastFailureStatus();

    ProbeDescriptor getDescriptor();

}
