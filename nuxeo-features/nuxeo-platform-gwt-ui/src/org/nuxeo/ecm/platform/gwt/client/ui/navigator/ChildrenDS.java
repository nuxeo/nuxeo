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

package org.nuxeo.ecm.platform.gwt.client.ui.navigator;

import org.nuxeo.ecm.platform.gwt.client.Framework;

import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.fields.DataSourceTextField;
import com.smartgwt.client.types.DSDataFormat;
import com.smartgwt.client.types.PromptStyle;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ChildrenDS extends NuxeoDataSource {
    
    private static ChildrenDS instance = new ChildrenDS(); 
    
    public static ChildrenDS getInstance() {
        return instance;
    }
    
    public ChildrenDS() {
        setID("listDS");        
        DataSourceTextField id = new DataSourceTextField("id", "Id");
        id.setPrimaryKey(true);
        DataSourceTextField type = new DataSourceTextField("type", "Type");
        
        //DataSourceTextField path = new DataSourceTextField("path", "Path");
        //DataSourceTextField name = new DataSourceTextField("name", "Name");
        DataSourceTextField title = new DataSourceTextField("title", "Title");

        setFields(type, title);
        setDataURL(Framework.getResourcePath("/files"));
        setDataFormat(DSDataFormat.JSON);
        DSRequest req = new DSRequest();
        req.setPromptStyle(PromptStyle.CURSOR); // use custom cursor instead of showing a dialog
        req.setWillHandleError(false);
        
        setRequestProperties(req);
    }
            
}
