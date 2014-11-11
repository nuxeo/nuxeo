/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo
 */

package org.nuxeo.ecm.platform.ws.jaws;

public class NuxeoRemotingInterfaceProxy implements org.nuxeo.ecm.platform.ws.jaws.NuxeoRemotingInterface {
  private String _endpoint = null;
  private org.nuxeo.ecm.platform.ws.jaws.NuxeoRemotingInterface nuxeoRemotingInterface = null;
  
  public NuxeoRemotingInterfaceProxy() {
    _initNuxeoRemotingInterfaceProxy();
  }
  
  private void _initNuxeoRemotingInterfaceProxy() {
    try {
      nuxeoRemotingInterface = (new org.nuxeo.ecm.platform.ws.jaws.NuxeoRemotingServiceLocator()).getNuxeoRemotingInterfacePort();
      if (nuxeoRemotingInterface != null) {
        if (_endpoint != null)
          ((javax.xml.rpc.Stub)nuxeoRemotingInterface)._setProperty("javax.xml.rpc.service.endpoint.address", _endpoint);
        else
          _endpoint = (String)((javax.xml.rpc.Stub)nuxeoRemotingInterface)._getProperty("javax.xml.rpc.service.endpoint.address");
      }
      
    }
    catch (javax.xml.rpc.ServiceException serviceException) {}
  }
  
  public String getEndpoint() {
    return _endpoint;
  }
  
  public void setEndpoint(String endpoint) {
    _endpoint = endpoint;
    if (nuxeoRemotingInterface != null)
      ((javax.xml.rpc.Stub)nuxeoRemotingInterface)._setProperty("javax.xml.rpc.service.endpoint.address", _endpoint);
    
  }
  
  public org.nuxeo.ecm.platform.ws.jaws.NuxeoRemotingInterface getNuxeoRemotingInterface() {
    if (nuxeoRemotingInterface == null)
      _initNuxeoRemotingInterfaceProxy();
    return nuxeoRemotingInterface;
  }
  
  public java.lang.String connect(java.lang.String string_1, java.lang.String string_2) throws java.rmi.RemoteException, org.nuxeo.ecm.platform.ws.jaws.ClientException{
    if (nuxeoRemotingInterface == null)
      _initNuxeoRemotingInterfaceProxy();
    return nuxeoRemotingInterface.connect(string_1, string_2);
  }
  
  public void disconnect(java.lang.String string_1) throws java.rmi.RemoteException, org.nuxeo.ecm.platform.ws.jaws.ClientException{
    if (nuxeoRemotingInterface == null)
      _initNuxeoRemotingInterfaceProxy();
    nuxeoRemotingInterface.disconnect(string_1);
  }
  
  public org.nuxeo.ecm.platform.api.ws.jaws.DocumentDescriptor[] getChildren(java.lang.String string_1, java.lang.String string_2) throws java.rmi.RemoteException, org.nuxeo.ecm.platform.ws.jaws.ClientException{
    if (nuxeoRemotingInterface == null)
      _initNuxeoRemotingInterfaceProxy();
    return nuxeoRemotingInterface.getChildren(string_1, string_2);
  }
  
  public org.nuxeo.ecm.platform.api.ws.jaws.DocumentDescriptor getDocument(java.lang.String string_1, java.lang.String string_2) throws java.rmi.RemoteException, org.nuxeo.ecm.platform.ws.jaws.ClientException{
    if (nuxeoRemotingInterface == null)
      _initNuxeoRemotingInterfaceProxy();
    return nuxeoRemotingInterface.getDocument(string_1, string_2);
  }
  
  public org.nuxeo.ecm.core.api.security.jaws.ACE[] getDocumentACL(java.lang.String string_1, java.lang.String string_2) throws java.rmi.RemoteException, org.nuxeo.ecm.platform.ws.jaws.ClientException{
    if (nuxeoRemotingInterface == null)
      _initNuxeoRemotingInterfaceProxy();
    return nuxeoRemotingInterface.getDocumentACL(string_1, string_2);
  }
  
  public org.nuxeo.ecm.platform.api.ws.jaws.DocumentBlob[] getDocumentBlobs(java.lang.String string_1, java.lang.String string_2) throws java.rmi.RemoteException, org.nuxeo.ecm.platform.ws.jaws.ClientException{
    if (nuxeoRemotingInterface == null)
      _initNuxeoRemotingInterfaceProxy();
    return nuxeoRemotingInterface.getDocumentBlobs(string_1, string_2);
  }
  
  public org.nuxeo.ecm.platform.api.ws.jaws.DocumentProperty[] getDocumentNoBlobProperties(java.lang.String string_1, java.lang.String string_2) throws java.rmi.RemoteException, org.nuxeo.ecm.platform.ws.jaws.ClientException{
    if (nuxeoRemotingInterface == null)
      _initNuxeoRemotingInterfaceProxy();
    return nuxeoRemotingInterface.getDocumentNoBlobProperties(string_1, string_2);
  }
  
  public org.nuxeo.ecm.platform.api.ws.jaws.DocumentProperty[] getDocumentProperties(java.lang.String string_1, java.lang.String string_2) throws java.rmi.RemoteException, org.nuxeo.ecm.platform.ws.jaws.ClientException{
    if (nuxeoRemotingInterface == null)
      _initNuxeoRemotingInterfaceProxy();
    return nuxeoRemotingInterface.getDocumentProperties(string_1, string_2);
  }
  
  public java.lang.String[] getGroups(java.lang.String string_1, java.lang.String string_2) throws java.rmi.RemoteException, org.nuxeo.ecm.platform.ws.jaws.ClientException{
    if (nuxeoRemotingInterface == null)
      _initNuxeoRemotingInterfaceProxy();
    return nuxeoRemotingInterface.getGroups(string_1, string_2);
  }
  
  public java.lang.String getRelativePathAsString(java.lang.String string_1, java.lang.String string_2) throws java.rmi.RemoteException, org.nuxeo.ecm.platform.ws.jaws.ClientException{
    if (nuxeoRemotingInterface == null)
      _initNuxeoRemotingInterfaceProxy();
    return nuxeoRemotingInterface.getRelativePathAsString(string_1, string_2);
  }
  
  public java.lang.String getRepositoryName(java.lang.String string_1) throws java.rmi.RemoteException, org.nuxeo.ecm.platform.ws.jaws.ClientException{
    if (nuxeoRemotingInterface == null)
      _initNuxeoRemotingInterfaceProxy();
    return nuxeoRemotingInterface.getRepositoryName(string_1);
  }
  
  public org.nuxeo.ecm.platform.api.ws.jaws.DocumentDescriptor getRootDocument(java.lang.String string_1) throws java.rmi.RemoteException, org.nuxeo.ecm.platform.ws.jaws.ClientException{
    if (nuxeoRemotingInterface == null)
      _initNuxeoRemotingInterfaceProxy();
    return nuxeoRemotingInterface.getRootDocument(string_1);
  }
  
  public java.lang.String[] getUsers(java.lang.String string_1, java.lang.String string_2) throws java.rmi.RemoteException, org.nuxeo.ecm.platform.ws.jaws.ClientException{
    if (nuxeoRemotingInterface == null)
      _initNuxeoRemotingInterfaceProxy();
    return nuxeoRemotingInterface.getUsers(string_1, string_2);
  }
  
  public java.lang.String[] listGroups(java.lang.String string_1, int int_1, int int_2) throws java.rmi.RemoteException, org.nuxeo.ecm.platform.ws.jaws.ClientException{
    if (nuxeoRemotingInterface == null)
      _initNuxeoRemotingInterfaceProxy();
    return nuxeoRemotingInterface.listGroups(string_1, int_1, int_2);
  }
  
  public java.lang.String[] listUsers(java.lang.String string_1, int int_1, int int_2) throws java.rmi.RemoteException, org.nuxeo.ecm.platform.ws.jaws.ClientException{
    if (nuxeoRemotingInterface == null)
      _initNuxeoRemotingInterfaceProxy();
    return nuxeoRemotingInterface.listUsers(string_1, int_1, int_2);
  }
  
  public java.lang.String uploadDocument(java.lang.String string_1, java.lang.String string_2, java.lang.String string_3, java.lang.String[] string_4) throws java.rmi.RemoteException, org.nuxeo.ecm.platform.ws.jaws.ClientException{
    if (nuxeoRemotingInterface == null)
      _initNuxeoRemotingInterfaceProxy();
    return nuxeoRemotingInterface.uploadDocument(string_1, string_2, string_3, string_4);
  }
  
  
}