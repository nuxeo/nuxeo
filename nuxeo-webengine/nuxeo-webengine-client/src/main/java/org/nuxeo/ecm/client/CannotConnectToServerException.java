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
package org.nuxeo.ecm.client;

/**
 * @author matic
 *
 */
public class CannotConnectToServerException extends ContentManagerException {
    
    private static final long serialVersionUID = -5341514481335821610L;

    protected CannotConnectToServerException(String message, Exception e) {
        super(message,e);
    }

    public static CannotConnectToServerException wrap(String message, Exception e) {
        return new CannotConnectToServerException(message,e);
    }
}
