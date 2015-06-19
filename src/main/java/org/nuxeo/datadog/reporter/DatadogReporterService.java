/*
 * (C) Copyright 2006-2015 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     dmetzler
 */
package org.nuxeo.datadog.reporter;

/**
 * Service that controls the Datadog reporter.
 *
 * @since 7.4
 */
public interface DatadogReporterService {


    /**
     * Starts the datadog reporter if it's not started.
     *
     */
    void startReporter();

    /**
     * Stops the datadog reporter if it's started.
     *
     */
    void stopReporter();



}
