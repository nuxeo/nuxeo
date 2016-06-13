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
package org.nuxeo.connect.tools.report.management;

import java.io.IOException;

import javax.json.JsonObject;
import javax.management.JMException;

/**
 *
 *
 * @since 8.3
 */
public interface MXInvoker {

    JsonObject list()
            throws IOException, JMException;

    JsonObject search(String pattern)
            throws IOException, JMException;

    JsonObject read(String pattern)
            throws IOException, JMException;

    JsonObject exec(String pattern, String operation, Object... arguments)
            throws IOException, JMException;

    void destroy();

}