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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.webengine.gwt.client.ui.navigator;

import org.nuxeo.ecm.webengine.gwt.client.UI;
import org.nuxeo.ecm.webengine.gwt.client.http.HttpResponse;
import org.nuxeo.ecm.webengine.gwt.client.http.ServerException;
import org.nuxeo.ecm.webengine.gwt.client.ui.ContextListener;
import org.nuxeo.ecm.webengine.gwt.client.ui.HttpCommand;
import org.nuxeo.ecm.webengine.gwt.client.ui.model.DocumentRef;

import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.Response;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONValue;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Tree;
import com.google.gwt.user.client.ui.TreeImages;
import com.google.gwt.user.client.ui.TreeItem;
import com.google.gwt.user.client.ui.TreeListener;

/**
 * @author eugen
 *
 */
public class NavigatorTree extends Tree{

    protected String rootPath;

    /**
     * @param rootPath - the url of the repository
     * @param navigationRootPath - the document path of the root navigator
     */
    public NavigatorTree(String rootPath) {
        super();
        this.rootPath = rootPath;
        addTreeListener(new NavigatorTreeListener());
    }

    @Override
    protected void onAttach() {
        super.onAttach();
        updateTree(rootPath, null);
    }

    public NavigatorTree(TreeImages images, boolean useLeafImages) {
        super(images, useLeafImages);
    }

    public NavigatorTree(TreeImages images) {
        super(images);
    }


    public DocumentRef getSelected(){
        TreeItem item = getSelectedItem();
        if ( item != null ){
            return getDocumentRef(item);
        }
        return null;
    }

    public DocumentRef getDocumentRef(TreeItem item){
        return (DocumentRef) item.getUserObject();
    }

    public String getSelectedUrl() {
        DocumentRef docRef = getSelected();
        if ( docRef  != null ){
            return getUrl(docRef.getPath());
        }
        return null;
    }

    protected String getUrl(String docPath) {
        return rootPath + docPath;
    }

    public String getUrl(TreeItem item){
        DocumentRef docRef = (DocumentRef) item.getUserObject();
        if ( docRef != null ) {
            return getUrl(docRef.getPath());
        }
        return null;
    }

    // TODO add a method that will select a specified node
    protected TreeItem createNode(DocumentRef obj){
        TreeItem node = new TreeItem();
        node.setUserObject(obj);
        String title = obj.getTitle();
        node.setText(title);
        if( obj.isFolderish() ){
            TreeItem fake = new TreeItem("fake");
            node.addItem(fake);
        }
        return node;

    }

    void updateTree(JSONArray array){
        for ( int i = 0, len = array.size() ; i < len ; i++){
            JSONObject obj = array.get(i).isObject();
            if( obj != null ) {
                TreeItem treeItem = createNode(new DocumentRef(obj));
                this.addItem(treeItem);
            }
        }
    }

    void updateTree(JSONArray array, TreeItem treeItem){
        treeItem.removeItems();
        for ( int i = 0, len = array.size() ; i < len ; i++){
            JSONObject obj = array.get(i).isObject();
            if( obj != null ) {
                TreeItem ti = createNode(new DocumentRef(obj));
                treeItem.addItem(ti);
            }
        }

    }

    public void updateTree(String path, final TreeItem item){
        //TODO the path should be computed by the command
        new GetChildrenCommand(path+"/@gwt?children=true", item).execute();
    }


    class NavigatorTreeListener implements TreeListener{

        public void onTreeItemSelected(TreeItem item) {
        }

        public void onTreeItemStateChanged(TreeItem item) {
            if ( item.getState()) {
                // check if node has been expanded
                if ( item.getChildCount() == 1 && "fake".equals(item.getChild(0).getText())) {
                    DocumentRef obj = (DocumentRef) item.getUserObject();
                    if ( obj != null ){
                        String s = obj.getPath();
                        updateTree(rootPath + s, item);
                    }
                }
            }
        }

    }


    public void refreshSelected() {
        TreeItem ti = getSelectedItem();
        refreshItem(ti);
    };

    public void refreshItem(TreeItem item) {
        DocumentRef docRef = getDocumentRef(item);
        if ( docRef != null ){
            String url = getUrl(docRef.getPath());
            updateTree(url, item);
        }

    };

    class GetChildrenCommand extends HttpCommand {
        protected TreeItem item;
        protected String path;
        public GetChildrenCommand(String path, TreeItem item) {
            super (null, 100);
            this.item = item;
            this.path = path;
        }
        @Override
        protected void doExecute() throws Throwable {
            get(path).send();
        }
        @Override
        public void onSuccess(HttpResponse response) {
            // parse the response text into JSON
            String text = response.getText();
            JSONValue jsonValue = JSONParser.parse(text);
            JSONArray jsonArray = jsonValue.isArray();
            if (jsonArray != null) {
                if ( item == null ){
                    updateTree(jsonArray);
                } else {
                    updateTree(jsonArray, item);
                }
            }
        }

        @Override
        public void onResponseReceived(Request request, Response response) {
            // TODO Auto-generated method stub
            super.onResponseReceived(request, response);
        }

        @Override
        public void onFailure(Throwable cause) {
            super.onFailure(cause);
            if( cause instanceof ServerException){
                ServerException e = (ServerException) cause;
                Response response = e.getResponse();
                if ( response.getStatusCode() == 401 ){
                    Window.alert("connection timeout. your are not logged");
                    UI.fireEvent(ContextListener.LOGOUT);
                }
            }
        }
    }


}
