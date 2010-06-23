/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
 * 
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the GNU Lesser General Public License (LGPL)
 * version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 * 
 * Contributors: Sun Seng David TAN (a.k.a. sunix) <stan@nuxeo.com>
 */
package org.nuxeo.ecm.platform.lock.api;

import java.net.URI;

import org.nuxeo.ecm.core.api.ClientException;

/**
 * Generic lock exception.
 * 
 * @author Sun Seng David TAN (a.k.a. sunix) <stan@nuxeo.com>
 * 
 */
public class LockException extends ClientException {

    private static final long serialVersionUID = 1L;

    public final URI resource;

    public LockException(String msg, URI resource) {
        super(resource + " : " + msg);
        this.resource = resource;
    }

    public LockException(String msg, Throwable e, URI resource) {
        super(msg, e);
        this.resource = resource;
    }

}
