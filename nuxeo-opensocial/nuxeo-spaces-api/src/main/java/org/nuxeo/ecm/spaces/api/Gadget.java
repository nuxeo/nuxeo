package org.nuxeo.ecm.spaces.api;

import java.util.Map;

public interface Gadget {

  /**
   * Unique identifier of a gadget
   * @return
   */
  String getId();
  /**
   * name
   * @return
   */
  String getName();
/**
 * description
 * @return
 */
  String getDescription();
  /**
   * title
   * @return
   */
  String getTitle();

  /**
   * creator name
   * @return
   */
  String getOwner();

  /**
   * use category
   * @return
   */
  //@Deprecated
  //String getType();

  /**
   * category of a gadget
   * @return
   */
  String getCategory();

  /**
   * preferences values
   * @return
   */
  public Map<String, String> getPreferences();

  /**
   * Key corresponding to the place where the gadget will be positionned in the view
   * @return
   */
  String getPlaceID();

  /**
   * Relative position in the parent container at the place id  "getPlaceID()"
   * @return
   */
  int getPosition();

  /**
   * Determines if the display state of the gadget
   * @return
   */
  boolean isCollapsed();


  public boolean isEqualTo(Gadget gadget);
}
