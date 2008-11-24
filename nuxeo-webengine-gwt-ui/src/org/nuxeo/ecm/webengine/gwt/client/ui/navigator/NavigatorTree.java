package org.nuxeo.ecm.webengine.gwt.client.ui.navigator;

import org.nuxeo.ecm.webengine.gwt.client.UI;
import org.nuxeo.ecm.webengine.gwt.client.ui.model.DocumentRef;

import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.http.client.URL;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONValue;
import com.google.gwt.user.client.ui.Tree;
import com.google.gwt.user.client.ui.TreeImages;
import com.google.gwt.user.client.ui.TreeItem;
import com.google.gwt.user.client.ui.TreeListener;

/**
 * @author eugen
 *
 */
public class NavigatorTree extends Tree{

    String repositoryUrl;
    String navigatorRootPath;

    /**
     * @param repositoryUrl - the url of the repository
     * @param navigationRootPath - the document path of the root navigator
     */
    public NavigatorTree(String repositoryUrl, String navigatorRootPath) {
        super();
        this.repositoryUrl = repositoryUrl;
        this.navigatorRootPath = navigatorRootPath;
        updateTree(repositoryUrl, null);
        addTreeListener(new NavigatorTreeListener());
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
        if( docPath != null && docPath.startsWith(navigatorRootPath)) {
            String deltaPath = docPath.substring(navigatorRootPath.length());
            return repositoryUrl + deltaPath;
        }
        return null;
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

    public JSONArray updateTree(String path, final TreeItem item){
        String p = URL.encode(path+"/@json?children=true");
        RequestBuilder builder = new RequestBuilder(RequestBuilder.GET, URL.encode(p));
        try {
            RequestCallback callback = new RequestCallback(){

                public void onError(Request request, Throwable exception) {
                }

                public void onResponseReceived(Request request,
                        Response response) {
                    if (200 == response.getStatusCode()) {
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
                    } else {
                        // Handle the error.  Can get the status text from response.getStatusText()
                    }

                }

            };
            Request request = builder.sendRequest(null, callback);
        } catch (RequestException e) {
            e.printStackTrace();
        }
        return null;
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
                        if( s.startsWith(navigatorRootPath)) {
                            String deltaPath = s.substring(navigatorRootPath.length());
                            updateTree(repositoryUrl + deltaPath, item);
                        }
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


}

