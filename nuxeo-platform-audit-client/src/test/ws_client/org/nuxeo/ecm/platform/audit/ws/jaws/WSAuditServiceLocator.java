/**
 * WSAuditServiceLocator.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package org.nuxeo.ecm.platform.audit.ws.jaws;

public class WSAuditServiceLocator extends org.apache.axis.client.Service implements org.nuxeo.ecm.platform.audit.ws.jaws.WSAuditService {

    public WSAuditServiceLocator() {
    }


    public WSAuditServiceLocator(org.apache.axis.EngineConfiguration config) {
        super(config);
    }

    public WSAuditServiceLocator(java.lang.String wsdlLoc, javax.xml.namespace.QName sName) throws javax.xml.rpc.ServiceException {
        super(wsdlLoc, sName);
    }

    // Use to get a proxy class for WSAuditInterfacePort
    private java.lang.String WSAuditInterfacePort_address = "http://boka:8080/nuxeo-platform-audit-client-5/WSAuditBean";

    public java.lang.String getWSAuditInterfacePortAddress() {
        return WSAuditInterfacePort_address;
    }

    // The WSDD service name defaults to the port name.
    private java.lang.String WSAuditInterfacePortWSDDServiceName = "WSAuditInterfacePort";

    public java.lang.String getWSAuditInterfacePortWSDDServiceName() {
        return WSAuditInterfacePortWSDDServiceName;
    }

    public void setWSAuditInterfacePortWSDDServiceName(java.lang.String name) {
        WSAuditInterfacePortWSDDServiceName = name;
    }

    public org.nuxeo.ecm.platform.audit.ws.jaws.WSAuditInterface getWSAuditInterfacePort() throws javax.xml.rpc.ServiceException {
       java.net.URL endpoint;
        try {
            endpoint = new java.net.URL(WSAuditInterfacePort_address);
        }
        catch (java.net.MalformedURLException e) {
            throw new javax.xml.rpc.ServiceException(e);
        }
        return getWSAuditInterfacePort(endpoint);
    }

    public org.nuxeo.ecm.platform.audit.ws.jaws.WSAuditInterface getWSAuditInterfacePort(java.net.URL portAddress) throws javax.xml.rpc.ServiceException {
        try {
            org.nuxeo.ecm.platform.audit.ws.jaws.WSAuditInterfaceBindingStub _stub = new org.nuxeo.ecm.platform.audit.ws.jaws.WSAuditInterfaceBindingStub(portAddress, this);
            _stub.setPortName(getWSAuditInterfacePortWSDDServiceName());
            return _stub;
        }
        catch (org.apache.axis.AxisFault e) {
            return null;
        }
    }

    public void setWSAuditInterfacePortEndpointAddress(java.lang.String address) {
        WSAuditInterfacePort_address = address;
    }

    /**
     * For the given interface, get the stub implementation.
     * If this service has no port for the given interface,
     * then ServiceException is thrown.
     */
    public java.rmi.Remote getPort(Class serviceEndpointInterface) throws javax.xml.rpc.ServiceException {
        try {
            if (org.nuxeo.ecm.platform.audit.ws.jaws.WSAuditInterface.class.isAssignableFrom(serviceEndpointInterface)) {
                org.nuxeo.ecm.platform.audit.ws.jaws.WSAuditInterfaceBindingStub _stub = new org.nuxeo.ecm.platform.audit.ws.jaws.WSAuditInterfaceBindingStub(new java.net.URL(WSAuditInterfacePort_address), this);
                _stub.setPortName(getWSAuditInterfacePortWSDDServiceName());
                return _stub;
            }
        }
        catch (java.lang.Throwable t) {
            throw new javax.xml.rpc.ServiceException(t);
        }
        throw new javax.xml.rpc.ServiceException("There is no stub implementation for the interface:  " + (serviceEndpointInterface == null ? "null" : serviceEndpointInterface.getName()));
    }

    /**
     * For the given interface, get the stub implementation.
     * If this service has no port for the given interface,
     * then ServiceException is thrown.
     */
    public java.rmi.Remote getPort(javax.xml.namespace.QName portName, Class serviceEndpointInterface) throws javax.xml.rpc.ServiceException {
        if (portName == null) {
            return getPort(serviceEndpointInterface);
        }
        java.lang.String inputPortName = portName.getLocalPart();
        if ("WSAuditInterfacePort".equals(inputPortName)) {
            return getWSAuditInterfacePort();
        }
        else  {
            java.rmi.Remote _stub = getPort(serviceEndpointInterface);
            ((org.apache.axis.client.Stub) _stub).setPortName(portName);
            return _stub;
        }
    }

    public javax.xml.namespace.QName getServiceName() {
        return new javax.xml.namespace.QName("http://ws.audit.platform.ecm.nuxeo.org/jaws", "WSAuditService");
    }

    private java.util.HashSet ports = null;

    public java.util.Iterator getPorts() {
        if (ports == null) {
            ports = new java.util.HashSet();
            ports.add(new javax.xml.namespace.QName("http://ws.audit.platform.ecm.nuxeo.org/jaws", "WSAuditInterfacePort"));
        }
        return ports.iterator();
    }

    /**
    * Set the endpoint address for the specified port name.
    */
    public void setEndpointAddress(java.lang.String portName, java.lang.String address) throws javax.xml.rpc.ServiceException {
        
if ("WSAuditInterfacePort".equals(portName)) {
            setWSAuditInterfacePortEndpointAddress(address);
        }
        else 
{ // Unknown Port Name
            throw new javax.xml.rpc.ServiceException(" Cannot set Endpoint Address for Unknown Port" + portName);
        }
    }

    /**
    * Set the endpoint address for the specified port name.
    */
    public void setEndpointAddress(javax.xml.namespace.QName portName, java.lang.String address) throws javax.xml.rpc.ServiceException {
        setEndpointAddress(portName.getLocalPart(), address);
    }

}
