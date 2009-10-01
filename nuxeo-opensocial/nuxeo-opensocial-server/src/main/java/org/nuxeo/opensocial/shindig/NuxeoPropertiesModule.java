package org.nuxeo.opensocial.shindig;

import org.apache.shindig.common.PropertiesModule;
import org.nuxeo.runtime.api.Framework;

public class NuxeoPropertiesModule extends PropertiesModule {

  public NuxeoPropertiesModule() {
    super(Framework.getProperties());
  }
}
