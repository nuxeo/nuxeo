/**
 * NuxeoRemotingInterface.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.3 Oct 05, 2005 (05:23:37 EDT) WSDL2Java emitter.
 */

package org.nuxeo.ecm.platform.ws.jaws;

public interface NuxeoRemotingInterface extends java.rmi.Remote {
    public java.lang.String connect(java.lang.String string_1, java.lang.String string_2) throws java.rmi.RemoteException, org.nuxeo.ecm.platform.ws.jaws.ClientException;
    public void disconnect(java.lang.String string_1) throws java.rmi.RemoteException, org.nuxeo.ecm.platform.ws.jaws.ClientException;
    public org.nuxeo.ecm.platform.api.ws.jaws.DocumentDescriptor[] getChildren(java.lang.String string_1, java.lang.String string_2) throws java.rmi.RemoteException, org.nuxeo.ecm.platform.ws.jaws.ClientException;
    public org.nuxeo.ecm.platform.api.ws.jaws.DocumentDescriptor getDocument(java.lang.String string_1, java.lang.String string_2) throws java.rmi.RemoteException, org.nuxeo.ecm.platform.ws.jaws.ClientException;
    public org.nuxeo.ecm.core.api.security.jaws.ACE[] getDocumentACL(java.lang.String string_1, java.lang.String string_2) throws java.rmi.RemoteException, org.nuxeo.ecm.platform.ws.jaws.ClientException;
    public org.nuxeo.ecm.platform.api.ws.jaws.DocumentBlob[] getDocumentBlobs(java.lang.String string_1, java.lang.String string_2) throws java.rmi.RemoteException, org.nuxeo.ecm.platform.ws.jaws.ClientException;
    public org.nuxeo.ecm.platform.api.ws.jaws.DocumentProperty[] getDocumentNoBlobProperties(java.lang.String string_1, java.lang.String string_2) throws java.rmi.RemoteException, org.nuxeo.ecm.platform.ws.jaws.ClientException;
    public org.nuxeo.ecm.platform.api.ws.jaws.DocumentProperty[] getDocumentProperties(java.lang.String string_1, java.lang.String string_2) throws java.rmi.RemoteException, org.nuxeo.ecm.platform.ws.jaws.ClientException;
    public java.lang.String[] getGroups(java.lang.String string_1, java.lang.String string_2) throws java.rmi.RemoteException, org.nuxeo.ecm.platform.ws.jaws.ClientException;
    public java.lang.String getRelativePathAsString(java.lang.String string_1, java.lang.String string_2) throws java.rmi.RemoteException, org.nuxeo.ecm.platform.ws.jaws.ClientException;
    public java.lang.String getRepositoryName(java.lang.String string_1) throws java.rmi.RemoteException, org.nuxeo.ecm.platform.ws.jaws.ClientException;
    public org.nuxeo.ecm.platform.api.ws.jaws.DocumentDescriptor getRootDocument(java.lang.String string_1) throws java.rmi.RemoteException, org.nuxeo.ecm.platform.ws.jaws.ClientException;
    public java.lang.String[] getUsers(java.lang.String string_1, java.lang.String string_2) throws java.rmi.RemoteException, org.nuxeo.ecm.platform.ws.jaws.ClientException;
    public java.lang.String[] listGroups(java.lang.String string_1, int int_1, int int_2) throws java.rmi.RemoteException, org.nuxeo.ecm.platform.ws.jaws.ClientException;
    public java.lang.String[] listUsers(java.lang.String string_1, int int_1, int int_2) throws java.rmi.RemoteException, org.nuxeo.ecm.platform.ws.jaws.ClientException;
    public java.lang.String uploadDocument(java.lang.String string_1, java.lang.String string_2, java.lang.String string_3, java.lang.String[] string_4) throws java.rmi.RemoteException, org.nuxeo.ecm.platform.ws.jaws.ClientException;
}
