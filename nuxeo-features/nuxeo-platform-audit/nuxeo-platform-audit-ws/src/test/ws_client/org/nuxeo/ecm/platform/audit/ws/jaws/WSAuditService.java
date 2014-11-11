/**
 * WSAuditService.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package org.nuxeo.ecm.platform.audit.ws.jaws;

public interface WSAuditService extends javax.xml.rpc.Service {
    public java.lang.String getWSAuditInterfacePortAddress();

    public org.nuxeo.ecm.platform.audit.ws.jaws.WSAuditInterface getWSAuditInterfacePort() throws javax.xml.rpc.ServiceException;

    public org.nuxeo.ecm.platform.audit.ws.jaws.WSAuditInterface getWSAuditInterfacePort(java.net.URL portAddress) throws javax.xml.rpc.ServiceException;
}
