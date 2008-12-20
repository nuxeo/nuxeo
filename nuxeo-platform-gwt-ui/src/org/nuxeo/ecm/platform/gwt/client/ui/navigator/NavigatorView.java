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

import java.util.Map;

import org.nuxeo.ecm.platform.gwt.client.Framework;
import org.nuxeo.ecm.platform.gwt.client.SmartClient;
import org.nuxeo.ecm.platform.gwt.client.ui.ControlContainer;
import org.nuxeo.ecm.platform.gwt.client.ui.SmartView;
import org.nuxeo.ecm.platform.gwt.client.ui.UI;

import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.ImgButton;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.grid.events.CellDoubleClickEvent;
import com.smartgwt.client.widgets.grid.events.CellDoubleClickHandler;
import com.smartgwt.client.widgets.tree.TreeGrid;
import com.smartgwt.client.widgets.tree.TreeNode;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class NavigatorView extends SmartView implements ControlContainer {

    protected TreeNode[] roots;

    public NavigatorView() {
        super("navigator");
    }
    
    @Override
    public String getTitle() {
        return "Navigator";
    }

    @Override
    protected TreeGrid createWidget() {
        TreeGrid tree = new TreeGrid();
        tree.setAutoFetchData(true);
        tree.setShowHeader(false);
        tree.setHeight100();
        tree.setWidth100();
        tree.setAnimateFolders(Boolean.parseBoolean(Framework.getSetting("animations", "false")));
        tree.setDataSource(TreeDS.getInstance());
        Map<String,String> repoRoots = Framework.getRepositoryRoots();
        roots = new TreeNode[repoRoots.size()];
        int i = 0;
        for (Map.Entry<String,String> entry : repoRoots.entrySet()) {
            TreeNode root = new TreeNode();
            String id = entry.getKey();
            String val = entry.getValue();
            root.setID(id);
            root.setTitle(val);
            root.setName(val);
            roots[i++] = root;
        } 
        tree.setInitialData(roots);        
        tree.addCellDoubleClickHandler(new DoubleClickHandler());
        return tree;
    }
    
    public TreeGrid getTree() {
        return (TreeGrid)getWidget();
    }
    

    @Override
    public void refresh() {
        if (widget != null) {
            for (TreeNode root : roots) {
                SmartClient.unloadChildren(getTree().getData(), root);
            }
        }
    }

    public Canvas[] getControls() {
        
        ImgButton refreshButton = new ImgButton();
        refreshButton.setSrc(Framework.getSkinPath("/images/refresh.png"));
        refreshButton.setSize(16);
        refreshButton.setShowRollOver(false);
        refreshButton.setShowDown(false);
        // show tooltip
        refreshButton.setCanHover(true);
        //refreshButton.setHoverHeight(1);
        refreshButton.setPrompt("Refresh");
        // add click handler
        refreshButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                refresh();
            }
        });
        
        return new Canvas[] {
                refreshButton
        };
        
    }
    
    class DoubleClickHandler implements CellDoubleClickHandler {
        public void onCellDoubleClick(CellDoubleClickEvent event) {
            ListGridRecord record = event.getRecord();
            UI.openDocument(record.getAttribute("id"));
        }        
    }
    
}
