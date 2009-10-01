package org.nuxeo.opensocial.container.factory.utils;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.runtime.api.Framework;

public class CoreSessionHelper {

  public static final String DEFAULT_REPOSITORY_NAME = "default";

  public static CoreSession getCoreSession(String repositoryName)
      throws Exception {
    RepositoryManager m = Framework.getService(RepositoryManager.class);
    if (repositoryName == null)
      return m.getRepository(DEFAULT_REPOSITORY_NAME)
          .open();
    else
      return m.getRepository(repositoryName)
          .open();

  }
}
