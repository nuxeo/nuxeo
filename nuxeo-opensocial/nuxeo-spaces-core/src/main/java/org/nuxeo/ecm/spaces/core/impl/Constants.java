package org.nuxeo.ecm.spaces.core.impl;

public interface Constants {// TODO à compléter

  public interface Document {
    String DOCUMENT_TITLE = "dc:title";
    String DOCUMENT_DESCRIPTION = "dc:description";
    String DOCUMENT_CREATOR = "dc:creator";
  }

  public interface Univers {
    String TYPE = "Univers";
    String ROOT_PATH="/default-domain/workspaces/galaxy";
  }

  public interface Space {
    String TYPE = "Space";
    String SPACE_THEME="space:theme";
    String SPACE_LAYOUT="space:layout";
    String SPACE_CATEGORY="space:categoryId";
  }

  public interface Gadget {
    String TYPE = "Gadget";
    //String GADGET_TYPE = "gadget:type";
    String GADGET_CATEGORY = "gadget:category";
    String GADGET_PLACEID = "gadget:placeID";// html division id
    String GADGET_POSITION = "gadget:position";// position in the div
    String GADGET_COLLAPSED = "gadget:collapsed";// is the gadget collapsed
    String GADGET_PREFERENCES="gadget:props";
  }
}
