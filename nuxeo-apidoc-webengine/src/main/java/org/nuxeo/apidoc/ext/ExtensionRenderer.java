package org.nuxeo.apidoc.ext;

public interface ExtensionRenderer {

    String getViewName();

    Object getRenderObject(String xml);

    Object getRenderObjectByIndex(int idx);

}
