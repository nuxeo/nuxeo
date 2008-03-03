/**
 * ACE.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.3 Oct 05, 2005 (05:23:37 EDT) WSDL2Java emitter.
 */

package org.nuxeo.ecm.core.api.security.jaws;

public class ACE  implements java.io.Serializable {
    private boolean denied;

    private boolean granted;

    private java.lang.String permission;

    private java.lang.String username;

    public ACE() {
    }

    public ACE(
           boolean denied,
           boolean granted,
           java.lang.String permission,
           java.lang.String username) {
           this.denied = denied;
           this.granted = granted;
           this.permission = permission;
           this.username = username;
    }


    /**
     * Gets the denied value for this ACE.
     * 
     * @return denied
     */
    public boolean isDenied() {
        return denied;
    }


    /**
     * Sets the denied value for this ACE.
     * 
     * @param denied
     */
    public void setDenied(boolean denied) {
        this.denied = denied;
    }


    /**
     * Gets the granted value for this ACE.
     * 
     * @return granted
     */
    public boolean isGranted() {
        return granted;
    }


    /**
     * Sets the granted value for this ACE.
     * 
     * @param granted
     */
    public void setGranted(boolean granted) {
        this.granted = granted;
    }


    /**
     * Gets the permission value for this ACE.
     * 
     * @return permission
     */
    public java.lang.String getPermission() {
        return permission;
    }


    /**
     * Sets the permission value for this ACE.
     * 
     * @param permission
     */
    public void setPermission(java.lang.String permission) {
        this.permission = permission;
    }


    /**
     * Gets the username value for this ACE.
     * 
     * @return username
     */
    public java.lang.String getUsername() {
        return username;
    }


    /**
     * Sets the username value for this ACE.
     * 
     * @param username
     */
    public void setUsername(java.lang.String username) {
        this.username = username;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof ACE)) return false;
        ACE other = (ACE) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = true && 
            this.denied == other.isDenied() &&
            this.granted == other.isGranted() &&
            ((this.permission==null && other.getPermission()==null) || 
             (this.permission!=null &&
              this.permission.equals(other.getPermission()))) &&
            ((this.username==null && other.getUsername()==null) || 
             (this.username!=null &&
              this.username.equals(other.getUsername())));
        __equalsCalc = null;
        return _equals;
    }

    private boolean __hashCodeCalc = false;
    public synchronized int hashCode() {
        if (__hashCodeCalc) {
            return 0;
        }
        __hashCodeCalc = true;
        int _hashCode = 1;
        _hashCode += (isDenied() ? Boolean.TRUE : Boolean.FALSE).hashCode();
        _hashCode += (isGranted() ? Boolean.TRUE : Boolean.FALSE).hashCode();
        if (getPermission() != null) {
            _hashCode += getPermission().hashCode();
        }
        if (getUsername() != null) {
            _hashCode += getUsername().hashCode();
        }
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(ACE.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://security.api.core.ecm.nuxeo.org/jaws", "ACE"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("denied");
        elemField.setXmlName(new javax.xml.namespace.QName("http://security.api.core.ecm.nuxeo.org/jaws", "denied"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "boolean"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("granted");
        elemField.setXmlName(new javax.xml.namespace.QName("http://security.api.core.ecm.nuxeo.org/jaws", "granted"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "boolean"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("permission");
        elemField.setXmlName(new javax.xml.namespace.QName("http://security.api.core.ecm.nuxeo.org/jaws", "permission"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("username");
        elemField.setXmlName(new javax.xml.namespace.QName("http://security.api.core.ecm.nuxeo.org/jaws", "username"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
    }

    /**
     * Return type metadata object
     */
    public static org.apache.axis.description.TypeDesc getTypeDesc() {
        return typeDesc;
    }

    /**
     * Get Custom Serializer
     */
    public static org.apache.axis.encoding.Serializer getSerializer(
           java.lang.String mechType, 
           java.lang.Class _javaType,  
           javax.xml.namespace.QName _xmlType) {
        return 
          new  org.apache.axis.encoding.ser.BeanSerializer(
            _javaType, _xmlType, typeDesc);
    }

    /**
     * Get Custom Deserializer
     */
    public static org.apache.axis.encoding.Deserializer getDeserializer(
           java.lang.String mechType, 
           java.lang.Class _javaType,  
           javax.xml.namespace.QName _xmlType) {
        return 
          new  org.apache.axis.encoding.ser.BeanDeserializer(
            _javaType, _xmlType, typeDesc);
    }

}
