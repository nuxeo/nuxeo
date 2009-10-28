package org.nuxeo.opensocial.container.client;

import com.google.gwt.i18n.client.Messages;

/**
 * Internationalization of gwt container
 * ContainerMessages.properties
 * @author Guillaume Cusnieux
 */
public interface ContainerMessages extends Messages {
  String preferencesGadget(String gadgetTitle);
  String getLabel(String fieldLabel);
}
