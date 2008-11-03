package org.nuxeo.ecm.webengine.ui.tree;

public class FakeContentProvider implements ContentProvider {

    private static final long serialVersionUID = -5447072937714133528L;

    public Object[] getChildren(Object obj) {
        return null;
    }

    public Object[] getElements(Object input) {
        return null;
    }

    public String[] getFacets(Object object) {
        return null;
    }

    public String getLabel(Object obj) {
        return null;
    }

    public String getName(Object obj) {
        return null;
    }

    public boolean isContainer(Object obj) {
        return false;
    }

}
