package org.nuxeo.opensocial.container.client.bean;

import com.google.gwt.user.client.rpc.IsSerializable;

public class GadgetPosition implements IsSerializable {

  private static final long serialVersionUID = 1L;
  private String placeID;
  private int position;

  /**
   * Default construcor (Specification of Gwt)
   */
  public GadgetPosition() {

  }

  public GadgetPosition(String placeID, int position) {
    this.placeID = placeID;
    this.position = position;
  }

  public String getPlaceID() {
    return placeID;
  }

  public Integer getPosition() {
    return position;
  }

  public void setPlaceId(String id) {
    placeID = id;
  }

  public void setPosition(int pos) {
    this.position = pos;
  }

}
