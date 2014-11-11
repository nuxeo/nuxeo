/**
 * WSAuditInterface.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package org.nuxeo.ecm.platform.audit.ws.jaws;

public interface WSAuditInterface extends java.rmi.Remote {
    public java.lang.String connect(java.lang.String string_1, java.lang.String string_2) throws java.rmi.RemoteException, org.nuxeo.ecm.platform.audit.ws.jaws.ClientException;
    public void disconnect(java.lang.String string_1) throws java.rmi.RemoteException, org.nuxeo.ecm.platform.audit.ws.jaws.ClientException;
    public org.nuxeo.ecm.platform.audit.ws.api.jaws.ModifiedDocumentDescriptor[] listModifiedDocuments(java.lang.String string_1, java.lang.String string_2) throws java.rmi.RemoteException, org.nuxeo.ecm.platform.audit.ws.jaws.AuditException;
}
