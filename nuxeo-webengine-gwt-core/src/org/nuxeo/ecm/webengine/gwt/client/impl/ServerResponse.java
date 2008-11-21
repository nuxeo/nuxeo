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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.webengine.gwt.client.impl;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ServerResponse {

    protected int code;
    protected String status;
    protected String content;
    
    public ServerResponse(int code, String status, String content) {
        this.code = code;
        this.status = status;
        this.content = content;
    }
    
    /**
     * @return the status.
     */
    public String getStatus() {
        return status;
    }
    
    /**
     * @return the code.
     */
    public int getCode() {
        return code;
    }
    
    /**
     * @return the content.
     */
    public String getContent() {
        return content;
    }
    
}
