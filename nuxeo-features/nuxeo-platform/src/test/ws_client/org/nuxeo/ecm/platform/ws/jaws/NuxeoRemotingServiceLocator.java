/**
 * NuxeoRemotingServiceLocator.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.3 Oct 05, 2005 (05:23:37 EDT) WSDL2Java emitter.
 */

package org.nuxeo.ecm.platform.ws.jaws;

public class NuxeoRemotingServiceLocator extends org.apache.axis.client.Service implements org.nuxeo.ecm.platform.ws.jaws.NuxeoRemotingService {

    public NuxeoRemotingServiceLocator() {
    }


    public NuxeoRemotingServiceLocator(org.apache.axis.EngineConfiguration config) {
        super(config);
    }

    public NuxeoRemotingServiceLocator(java.lang.String wsdlLoc, javax.xml.namespace.QName sName) throws javax.xml.rpc.ServiceException {
        super(wsdlLoc, sName);
    }

    // Use to get a proxy class for NuxeoRemotingInterfacePort
    private java.lang.String NuxeoRemotingInterfacePort_address = "http://bongofury:8080/nuxeo-platform-5/NuxeoRemotingBean";

    public java.lang.String getNuxeoRemotingInterfacePortAddress() {
        return NuxeoRemotingInterfacePort_address;
    }

    // The WSDD service name defaults to the port name.
    private java.lang.String NuxeoRemotingInterfacePortWSDDServiceName = "NuxeoRemotingInterfacePort";

    public java.lang.String getNuxeoRemotingInterfacePortWSDDServiceName() {
        return NuxeoRemotingInterfacePortWSDDServiceName;
    }

    public void setNuxeoRemotingInterfacePortWSDDServiceName(java.lang.String name) {
        NuxeoRemotingInterfacePortWSDDServiceName = name;
    }

    public org.nuxeo.ecm.platform.ws.jaws.NuxeoRemotingInterface getNuxeoRemotingInterfacePort() throws javax.xml.rpc.ServiceException {
       java.net.URL endpoint;
        try {
            endpoint = new java.net.URL(NuxeoRemotingInterfacePort_address);
        }
        catch (java.net.MalformedURLException e) {
            throw new javax.xml.rpc.ServiceException(e);
        }
        return getNuxeoRemotingInterfacePort(endpoint);
    }

    public org.nuxeo.ecm.platform.ws.jaws.NuxeoRemotingInterface getNuxeoRemotingInterfacePort(java.net.URL portAddress) throws javax.xml.rpc.ServiceException {
        try {
            org.nuxeo.ecm.platform.ws.jaws.NuxeoRemotingInterfaceBindingStub _stub = new org.nuxeo.ecm.platform.ws.jaws.NuxeoRemotingInterfaceBindingStub(portAddress, this);
            _stub.setPortName(getNuxeoRemotingInterfacePortWSDDServiceName());
            return _stub;
        }
        catch (org.apache.axis.AxisFault e) {
            return null;
        }
    }

    public void setNuxeoRemotingInterfacePortEndpointAddress(java.lang.String address) {
        NuxeoRemotingInterfacePort_address = address;
    }

    /**
     * For the given interface, get the stub implementation.
     * If this service has no port for the given interface,
     * then ServiceException is thrown.
     */
    public java.rmi.Remote getPort(Class serviceEndpointInterface) throws javax.xml.rpc.ServiceException {
        try {
            if (org.nuxeo.ecm.platform.ws.jaws.NuxeoRemotingInterface.class.isAssignableFrom(serviceEndpointInterface)) {
                org.nuxeo.ecm.platform.ws.jaws.NuxeoRemotingInterfaceBindingStub _stub = new org.nuxeo.ecm.platform.ws.jaws.NuxeoRemotingInterfaceBindingStub(new java.net.URL(NuxeoRemotingInterfacePort_address), this);
                _stub.setPortName(getNuxeoRemotingInterfacePortWSDDServiceName());
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
        if ("NuxeoRemotingInterfacePort".equals(inputPortName)) {
            return getNuxeoRemotingInterfacePort();
        }
        else  {
            java.rmi.Remote _stub = getPort(serviceEndpointInterface);
            ((org.apache.axis.client.Stub) _stub).setPortName(portName);
            return _stub;
        }
    }

    public javax.xml.namespace.QName getServiceName() {
        return new javax.xml.namespace.QName("http://ws.platform.ecm.nuxeo.org/jaws", "NuxeoRemotingService");
    }

    private java.util.HashSet ports = null;

    public java.util.Iterator getPorts() {
        if (ports == null) {
            ports = new java.util.HashSet();
            ports.add(new javax.xml.namespace.QName("http://ws.platform.ecm.nuxeo.org/jaws", "NuxeoRemotingInterfacePort"));
        }
        return ports.iterator();
    }

    /**
    * Set the endpoint address for the specified port name.
    */
    public void setEndpointAddress(java.lang.String portName, java.lang.String address) throws javax.xml.rpc.ServiceException {
        
if ("NuxeoRemotingInterfacePort".equals(portName)) {
            setNuxeoRemotingInterfacePortEndpointAddress(address);
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
