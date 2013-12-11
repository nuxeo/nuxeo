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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public abstract class AbstractDWSItem implements DWSItem {

    protected String authorId = "unset";
    protected String editorId = "unset";

    protected String authorLogin;
    protected Date created;
    protected Date modified;
    protected String fileRef;
    protected String id;


    public AbstractDWSItem(String id, String authorLogin, Date created, Date modified, String fileRef) {
        this.id=id;
        this.authorLogin = authorLogin;
        this.created=created;
        this.modified=modified;
        this.fileRef=fileRef;
    }

    protected static DateFormat getDateFormat() {
        // not thread-safe so don't use a static instance
        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
    }

    public String getAuthorRef() {
        return authorId + ";#" + getAuthorLogin();
    }

    public String getEditorLogin() {
        return getAuthorLogin();
    }

    public String getEditorRef() {
        return editorId + ";#" + getEditorLogin();
    }

    public String getUniqueId() {
        return getId() + ";#{" + UUID.randomUUID().toString() +  "}";
    }

    public void updateReferences(List<User> users) {
        for (int i =0; i< users.size(); i++) {
            if (users.get(i).getLogin().equals(getAuthorLogin())) {
                //authorId = ""+ i+1;
                authorId = users.get(i).getId();
                break;
            }
        }
        for (int i =0; i< users.size(); i++) {
            if (users.get(i).getLogin().equals(getEditorLogin())) {
                //editorId = ""+ i+1;
                editorId =  users.get(i).getId();
                break;
            }
        }
    }

    public String getCreatedTS() {
        Date date = getCreated();
        if (date==null) {
            date = new Date(System.currentTimeMillis());
        }
        return getDateFormat().format(date);
    }

    public String getModifiedTS() {
        Date date = getModified();
        if (date==null) {
            date = getCreated();
        }
        if (date==null) {
            date = new Date(System.currentTimeMillis());
        }
        return getDateFormat().format(date);
    }

    public String getAuthorLogin() {
        return authorLogin;
    }

    public Date getCreated() {
        return created;
    }

    public String getId() {
        return id;
    }

    public Date getModified() {
        return modified;
    }

    public String getFileRef() {
        return fileRef;
    }

}
