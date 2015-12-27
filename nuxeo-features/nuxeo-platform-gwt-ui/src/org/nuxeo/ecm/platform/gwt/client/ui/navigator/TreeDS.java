/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.gwt.client.ui.navigator;

import org.nuxeo.ecm.platform.gwt.client.Framework;

import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.fields.DataSourceBooleanField;
import com.smartgwt.client.data.fields.DataSourceTextField;
import com.smartgwt.client.types.DSDataFormat;
import com.smartgwt.client.types.PromptStyle;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class TreeDS extends NuxeoDataSource {

    protected static TreeDS instance = new TreeDS();

    public static TreeDS getInstance() {
        return instance;
    }

    protected TreeDS() {
        setID("treeDS");
        DataSourceTextField id = new DataSourceTextField("id", "Id");
        id.setPrimaryKey(true);
        DataSourceTextField type = new DataSourceTextField("type", "Type");
        DataSourceTextField path = new DataSourceTextField("path", "Path");
        DataSourceTextField name = new DataSourceTextField("name", "Name");
        DataSourceTextField title = new DataSourceTextField("title", "Title");
        DataSourceBooleanField isFolder = new DataSourceBooleanField("isFolder", "Folder");
        DataSourceTextField parent = new DataSourceTextField("parentId", "ParentId");
        parent.setForeignKey("id");

        setFields(id, parent, isFolder, type, path, name, title);
        setDataURL(Framework.getResourcePath("/tree"));
        setDataFormat(DSDataFormat.JSON);
        DSRequest req = new DSRequest();
        req.setPromptStyle(PromptStyle.CURSOR); // use custom cursor instead of showing a dialog
        //req.setWillHandleError(true);

        setRequestProperties(req);
    }

}
