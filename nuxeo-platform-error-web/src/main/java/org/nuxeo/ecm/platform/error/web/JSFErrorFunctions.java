/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.error.web;

import org.nuxeo.ecm.core.api.ClientException;

/**
 * JSF functions triggering errors
 *
 * @author Anahide Tchertchian
 */
public class JSFErrorFunctions {

    public static String triggerCheckedError() throws ClientException {
        throw new ClientException("JSF function triggering a checked error");
    }

    public static String triggerUncheckedError() {
        throw new NullPointerException("JSF function triggering an unchecked error");
    }

}
