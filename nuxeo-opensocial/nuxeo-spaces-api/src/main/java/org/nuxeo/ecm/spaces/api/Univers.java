package org.nuxeo.ecm.spaces.api;


/**
 * Global container corresponding to a site . A univers can contain
 * <code>Space</code> elements . <code>Space</code> elements contained in this universe are
 * retrieved via the SpaceManager framework service : <br/><br/> SpaceManager service
 * = Framework.getService(SpaceManager.class)<br/> List&lt;Space&gt; spaces =
 * service.getSpacesForUnivers(univers,coreSession);
 */
 public interface Univers {

  /**
   * UID
   *
   * @return a unique identifier for a given instance of Univers implementation
   */
   String getId();

  /**
   * Universe name
   *
   * @return the name of this universe
   */
   String getName();

  /**
   * Universe title
   *
   * @return the title of this universe
   */
   String getTitle();

  /**
   * Universe description
   *
   * @return the description of this universe
   */
   String getDescription();


  /**
   * for comparison
   * @param space
   * @return
   */
  boolean isEqualTo(Univers univers);

}
