package com.leroymerlin.corp.fr.nuxeo.portal.testing;

import java.util.concurrent.Callable;

import org.nuxeo.ecm.core.api.CoreSession;

public abstract class SessionCall<T> implements Callable<T> {

  protected CoreSession session;

  public void setSession(CoreSession session) {
    this.session = session;
  }

}
