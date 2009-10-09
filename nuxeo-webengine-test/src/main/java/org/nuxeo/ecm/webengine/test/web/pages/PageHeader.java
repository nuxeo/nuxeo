package org.nuxeo.ecm.webengine.test.web.pages;


public interface PageHeader {

  PageHeader logout();

  void loginAs(String login, String password);

}
