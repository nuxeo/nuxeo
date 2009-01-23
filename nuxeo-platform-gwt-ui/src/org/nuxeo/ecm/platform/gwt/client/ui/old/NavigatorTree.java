package org.nuxeo.ecm.platform.gwt.client.ui.old;

import org.nuxeo.ecm.platform.gwt.client.Framework;
import org.nuxeo.ecm.platform.gwt.client.http.HttpResponse;
import org.nuxeo.ecm.platform.gwt.client.http.ServerException;
import org.nuxeo.ecm.platform.gwt.client.model.DocumentRef;
import org.nuxeo.ecm.platform.gwt.client.ui.HttpCommand;

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


    /**
     * @param rootPath - the url of the repository
     * @param navigationRootPath - the document path of the root navigator
     */
    public NavigatorTree() {
        super();
        addTreeListener(new NavigatorTreeListener());
    }
    
    @Override
    protected void onAttach() {
        super.onAttach();
        updateTree((TreeItem)null);
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
            return getDocRef(item);
        }
        return null;
    }

    public DocumentRef getDocRef(TreeItem item){
        return (DocumentRef) item.getUserObject();
    }




    // TODO add a method that will select a specified node
    protected TreeItem createNode(DocumentRef obj){
        TreeItem node = new TreeItem();
        node.setUserObject(obj);
        String title = obj.title;
        node.setText(title);
        if( obj.isFolder ){
            TreeItem fake = new TreeItem("Loading ...");
            node.addItem(fake);
        }
        return node;

    }

    void updateTree(JSONArray array){
        for ( int i = 0, len = array.size() ; i < len ; i++){
            JSONObject obj = array.get(i).isObject();
            if( obj != null ) {
                TreeItem treeItem = createNode(DocumentRef.fromJSON(obj));
                this.addItem(treeItem);
            }
        }
    }

    void updateTree(JSONArray array, TreeItem treeItem){
        treeItem.removeItems();
        for ( int i = 0, len = array.size() ; i < len ; i++){
            JSONObject obj = array.get(i).isObject();
            if( obj != null ) {
                TreeItem ti = createNode(DocumentRef.fromJSON(obj));
                treeItem.addItem(ti);
            }
        }

    }

    public void updateTree(final TreeItem item){
        new GetChildrenCommand(Framework.getResourcePath("/tree"), item).execute();
    }


    class NavigatorTreeListener implements TreeListener{

        public void onTreeItemSelected(TreeItem item) {
        }

        public void onTreeItemStateChanged(TreeItem item) {
            if ( item.getState()) {
                // check if node has been expanded
                if ( item.getChildCount() == 1 && "Loading ...".equals(item.getChild(0).getText())) {
                    DocumentRef obj = (DocumentRef) item.getUserObject();
                    if ( obj != null ){
                        updateTree(item);
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
        DocumentRef docRef = getDocRef(item);
        if ( docRef != null ){
            updateTree(item);
        }

    };

    class GetChildrenCommand extends HttpCommand {
        protected TreeItem item;
        protected String path;
        public GetChildrenCommand(String path, TreeItem item) {
            super (null, 100);
            this.item = item;
            this.path = path;
            if (item != null) {
                this.path = this.path + "?parentId="+((DocumentRef)item.getUserObject()).id;
            }
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
            JSONObject resp = jsonValue.isObject().get("response").isObject();
            if (resp.get("status").isNumber().doubleValue() < 0) { // error
                //TODO handle error
                Window.alert("Error received from server"+resp.get("status"));
                return;
            } 
            JSONArray jsonArray = resp.get("data").isArray();
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
                }
            }
        }
    }


}

