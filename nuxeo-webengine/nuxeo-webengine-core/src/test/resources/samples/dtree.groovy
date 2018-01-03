import org.nuxeo.common.utils.*;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.ecm.webengine.ui.json.*;
import org.nuxeo.ecm.webengine.ui.tree.*;
import org.nuxeo.ecm.webengine.ui.tree.directory.*; 


Context.getUserSession().getComponent(DirectoryTree.class, "myTree");

class DirTree extends DirectoryTree {

    protected ContentProvider getProvider(WebContext ctx) throws WebException {
        try {
            return new MyProvider(dir.getSession());
        } catch (DirectoryException e) {
            throw WebException.wrap(e);
        }
    }

}




def tree = new DirectoryTree("disciplines");

serializer = new MySerializer(Context);

//item = tree.findAndReveal("/disciplines");
item = tree.getRoot();
item.expand();
json = serializer.toJSON(item.getChildren())

out.println(json)


//new JSonDocTree(Context, "myDTree").select();




