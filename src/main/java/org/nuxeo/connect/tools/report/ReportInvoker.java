/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Stephane Lacoin at Nuxeo (aka matic)
 */
package org.nuxeo.connect.tools.report;

import java.io.IOException;
import java.nio.file.Path;

import javax.management.MXBean;

/**
 * Report invoker from outside
 *
 * @since 8.3
 */
@MXBean
public interface ReportInvoker {

    /**
     * Invokes a snapshot and stores result in the provided directory
     *
     * @return the file path where the snapshot is stored
     * @throws IOException
     * @since 8.3
     */
    Path snapshot(Path path) throws IOException;
}
