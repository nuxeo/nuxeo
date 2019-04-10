/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Thierry Delprat
 */

package org.nuxeo.wss.spi.dws;

import java.util.Date;

public class LinkImpl extends AbstractDWSItem implements Link {

    protected String comments;

    protected String url;

    public LinkImpl(String id, String authorLogin, Date created, Date modified, String fileRef, String comments,
            String url) {
        super(id, authorLogin, created, modified, fileRef);
        this.comments = comments;
        this.url = url;
    }

    public String getComments() {
        return comments;
    }

    public String getUrl() {
        return url;
    }

}
