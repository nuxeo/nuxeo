/**
 * NuxeoRemotingService.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.3 Oct 05, 2005 (05:23:37 EDT) WSDL2Java emitter.
 */

package org.nuxeo.ecm.platform.ws.jaws;

public interface NuxeoRemotingService extends javax.xml.rpc.Service {
    public java.lang.String getNuxeoRemotingInterfacePortAddress();

    public org.nuxeo.ecm.platform.ws.jaws.NuxeoRemotingInterface getNuxeoRemotingInterfacePort() throws javax.xml.rpc.ServiceException;

    public org.nuxeo.ecm.platform.ws.jaws.NuxeoRemotingInterface getNuxeoRemotingInterfacePort(java.net.URL portAddress) throws javax.xml.rpc.ServiceException;
}
