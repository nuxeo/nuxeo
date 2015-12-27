/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Nuxeo
 */

package org.nuxeo.ecm.platform.audit.ws.jaws;

public class WSAuditInterfaceProxy implements org.nuxeo.ecm.platform.audit.ws.jaws.WSAuditInterface {
  private String _endpoint = null;
  private org.nuxeo.ecm.platform.audit.ws.jaws.WSAuditInterface wSAuditInterface = null;
  
  public WSAuditInterfaceProxy() {
    _initWSAuditInterfaceProxy();
  }
  
  public WSAuditInterfaceProxy(String endpoint) {
    _endpoint = endpoint;
    _initWSAuditInterfaceProxy();
  }
  
  private void _initWSAuditInterfaceProxy() {
    try {
      wSAuditInterface = (new org.nuxeo.ecm.platform.audit.ws.jaws.WSAuditServiceLocator()).getWSAuditInterfacePort();
      if (wSAuditInterface != null) {
        if (_endpoint != null)
          ((javax.xml.rpc.Stub)wSAuditInterface)._setProperty("javax.xml.rpc.service.endpoint.address", _endpoint);
        else
          _endpoint = (String)((javax.xml.rpc.Stub)wSAuditInterface)._getProperty("javax.xml.rpc.service.endpoint.address");
      }
      
    }
    catch (javax.xml.rpc.ServiceException serviceException) {}
  }
  
  public String getEndpoint() {
    return _endpoint;
  }
  
  public void setEndpoint(String endpoint) {
    _endpoint = endpoint;
    if (wSAuditInterface != null)
      ((javax.xml.rpc.Stub)wSAuditInterface)._setProperty("javax.xml.rpc.service.endpoint.address", _endpoint);
    
  }
  
  public org.nuxeo.ecm.platform.audit.ws.jaws.WSAuditInterface getWSAuditInterface() {
    if (wSAuditInterface == null)
      _initWSAuditInterfaceProxy();
    return wSAuditInterface;
  }
  
  public java.lang.String connect(java.lang.String string_1, java.lang.String string_2) throws java.rmi.RemoteException, org.nuxeo.ecm.platform.audit.ws.jaws.ClientException{
    if (wSAuditInterface == null)
      _initWSAuditInterfaceProxy();
    return wSAuditInterface.connect(string_1, string_2);
  }
  
  public void disconnect(java.lang.String string_1) throws java.rmi.RemoteException, org.nuxeo.ecm.platform.audit.ws.jaws.ClientException{
    if (wSAuditInterface == null)
      _initWSAuditInterfaceProxy();
    wSAuditInterface.disconnect(string_1);
  }
  
  public org.nuxeo.ecm.platform.audit.ws.api.jaws.ModifiedDocumentDescriptor[] listModifiedDocuments(java.lang.String string_1, java.lang.String string_2) throws java.rmi.RemoteException, org.nuxeo.ecm.platform.audit.ws.jaws.AuditException{
    if (wSAuditInterface == null)
      _initWSAuditInterfaceProxy();
    return wSAuditInterface.listModifiedDocuments(string_1, string_2);
  }
  
  
}
