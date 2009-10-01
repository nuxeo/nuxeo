package com.leroymerlin.corp.fr.nuxeo.portal.testing;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.core.NXCore;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.local.LocalSession;
import org.nuxeo.ecm.core.model.Repository;

public class TestRepositoryHandler {

  private Repository repository;
  private String repositoryName;

  public TestRepositoryHandler(String name) {
    this.repositoryName = name;
  }

  public void openRepository() throws Exception {
    this.repository = NXCore.getRepositoryService()
        .getRepositoryManager()
        .getRepository(repositoryName);
  }

  public CoreSession openSessionAs(String userName) throws ClientException {
    Map<String, Serializable> ctx = new HashMap<String, Serializable>();
    ctx.put("username", userName);
    CoreSession coreSession = new LocalSession();
    coreSession.connect(repositoryName, ctx);
    return coreSession;
  }

  public CoreSession changeUser(CoreSession session, String newUser)
      throws ClientException {
    releaseSession(session);
    return openSessionAs(newUser);
  }

  public void releaseRepository() {
    if (repository != null) {
      repository.shutdown();
    }
  }

  public void releaseSession(CoreSession session) {
    CoreInstance.getInstance()
        .close(session);
  }
}
