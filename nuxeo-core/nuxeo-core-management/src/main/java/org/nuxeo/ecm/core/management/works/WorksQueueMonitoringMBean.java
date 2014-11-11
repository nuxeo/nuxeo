/*******************************************************************************
 * Copyright (c) 2006-2013 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.nuxeo.ecm.core.management.works;

import javax.management.MXBean;

@MXBean
public interface WorksQueueMonitoringMBean {

    int getScheduledCount();

    int getRunningCount();

    int getCompletedCount();

    String[] getScheduledWorks();

    String[] getRunningWorks();

}