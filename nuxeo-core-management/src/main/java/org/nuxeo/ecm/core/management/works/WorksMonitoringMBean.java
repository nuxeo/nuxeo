/*
 * Copyright (c) 2006-2013 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 */
package org.nuxeo.ecm.core.management.works;

/**
 * Manage works from the JMX console
 *
 * @since 5.8
 * @author Stephane Lacoin at Nuxeo (aka matic)
 *
 */
public interface WorksMonitoringMBean {


    /**
     * Enable/disable work schedule stack capture
     */
    void toggleScheduleStackCapture();

    /**
     * is work schedule stack capture enabled ?
     */
    boolean isScheduleStackCapture();

}
