package com.leroymerlin.corp.fr.nuxeo.portal.testing;

import java.util.List;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;

public class FakeNuxeoPrincipal implements NuxeoPrincipal {

  private static final long serialVersionUID = 1L;
  private String id;
  private String firstName;
  private String lastName;
  private DocumentModel model;

  public FakeNuxeoPrincipal(String id, String firstName, String lastName,
      String email, String listeLibelleRayon, String listeCodeRayon,
      String codeMagasin, String libelleMagasin, String mailUser,
      String mailHost, String region) {
    this.id = id;
    this.firstName = firstName;
    this.lastName = lastName;
    this.model = new FakeDocumentModel(email, listeLibelleRayon,
        listeCodeRayon, codeMagasin, libelleMagasin, mailUser, mailHost, region);

  }

  public FakeNuxeoPrincipal(String id, String firstName, String lastName) {
    this(id, firstName, lastName, null, null, null, null, null);
  }

  public FakeNuxeoPrincipal(String id, String firstName, String lastName,
      String email, String listeLibelleRayon, String listeCodeRayon,
      String codeMagasin, String libelleMagasin) {
    this(id, firstName, lastName, email, listeLibelleRayon, listeCodeRayon,
        codeMagasin, libelleMagasin, null, null, null);
  }

  public FakeNuxeoPrincipal(String id, String firstName, String lastName,
      String email, String listeLibelleRayon, String listeCodeRayon,
      String codeMagasin, String libelleMagasin, String mailUser,
      String mailHost) {
    this(id, firstName, lastName, email, listeLibelleRayon, listeCodeRayon,
        codeMagasin, libelleMagasin, mailUser, mailHost, null);
  }

  public List<String> getAllGroups() {
    return null;
  }

  public String getCompany() {
    return null;
  }

  public String getFirstName() {
    return firstName;
  }

  public List<String> getGroups() {
    return null;
  }

  public String getLastName() {
    return lastName;
  }

  public DocumentModel getModel() {
    return this.model;
  }

  public String getOriginatingUser() {
    return null;
  }

  public String getPassword() {
    return null;
  }

  public String getPrincipalId() {
    return id;
  }

  public List<String> getRoles() {
    return null;
  }

  public boolean isAdministrator() {
    return false;
  }

  public boolean isAnonymous() {
    return false;
  }

  public boolean isMemberOf(String group) {
    return false;
  }

  public void setCompany(String company) {
  }

  public void setFirstName(String firstName) {
    this.firstName = firstName;
  }

  public void setGroups(List<String> groups) {
  }

  public void setLastName(String lastName) {
    this.lastName = lastName;
  }

  public void setModel(DocumentModel model) throws ClientException {
  }

  public void setName(String name) {
  }

  public void setOriginatingUser(String originatingUser) {
  }

  public void setPassword(String password) {
  }

  public void setPrincipalId(String principalId) {
  }

  public void setRoles(List<String> roles) {
  }

  public String getName() {
    return id;
  }

}
