package com.leroymerlin.corp.fr.nuxeo.portal.testing;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;

public class WithUser {

  private final BaseWebTester baseWebTester;
  private final String user;
  private CoreSession session;

  public WithUser(String user, BaseWebTester baseWebTester) {
    this.user = user;
    this.baseWebTester = baseWebTester;
  }

  public <V> V call(SessionCall<V> callable) throws Exception {
    session = open();
    callable.setSession(session);
    try {
      V v = callable.call();
      session.save();
      return v;
    } catch (Exception e) {
      session.cancel();
      throw e;
    } finally {
      close(session);
    }
  }

  public void close(CoreSession session) {
    baseWebTester.getRepository()
        .releaseSession(session);
  }

  public CoreSession open() throws ClientException {
    return baseWebTester.getRepository()
        .openSessionAs(user);
  }

}
