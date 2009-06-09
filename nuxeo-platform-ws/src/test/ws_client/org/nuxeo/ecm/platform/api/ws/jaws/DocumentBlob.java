/**
 * DocumentBlob.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.3 Oct 05, 2005 (05:23:37 EDT) WSDL2Java emitter.
 */

package org.nuxeo.ecm.platform.api.ws.jaws;

public class DocumentBlob  implements java.io.Serializable {
    private byte[] blob;

    private java.lang.String encoding;

    private java.lang.String[] extensions;

    private java.lang.String mimeType;

    private java.lang.String name;

    public DocumentBlob() {
    }

    public DocumentBlob(
           byte[] blob,
           java.lang.String encoding,
           java.lang.String[] extensions,
           java.lang.String mimeType,
           java.lang.String name) {
           this.blob = blob;
           this.encoding = encoding;
           this.extensions = extensions;
           this.mimeType = mimeType;
           this.name = name;
    }


    /**
     * Gets the blob value for this DocumentBlob.
     * 
     * @return blob
     */
    public byte[] getBlob() {
        return blob;
    }


    /**
     * Sets the blob value for this DocumentBlob.
     * 
     * @param blob
     */
    public void setBlob(byte[] blob) {
        this.blob = blob;
    }


    /**
     * Gets the encoding value for this DocumentBlob.
     * 
     * @return encoding
     */
    public java.lang.String getEncoding() {
        return encoding;
    }


    /**
     * Sets the encoding value for this DocumentBlob.
     * 
     * @param encoding
     */
    public void setEncoding(java.lang.String encoding) {
        this.encoding = encoding;
    }


    /**
     * Gets the extensions value for this DocumentBlob.
     * 
     * @return extensions
     */
    public java.lang.String[] getExtensions() {
        return extensions;
    }


    /**
     * Sets the extensions value for this DocumentBlob.
     * 
     * @param extensions
     */
    public void setExtensions(java.lang.String[] extensions) {
        this.extensions = extensions;
    }

    public java.lang.String getExtensions(int i) {
        return this.extensions[i];
    }

    public void setExtensions(int i, java.lang.String _value) {
        this.extensions[i] = _value;
    }


    /**
     * Gets the mimeType value for this DocumentBlob.
     * 
     * @return mimeType
     */
    public java.lang.String getMimeType() {
        return mimeType;
    }


    /**
     * Sets the mimeType value for this DocumentBlob.
     * 
     * @param mimeType
     */
    public void setMimeType(java.lang.String mimeType) {
        this.mimeType = mimeType;
    }


    /**
     * Gets the name value for this DocumentBlob.
     * 
     * @return name
     */
    public java.lang.String getName() {
        return name;
    }


    /**
     * Sets the name value for this DocumentBlob.
     * 
     * @param name
     */
    public void setName(java.lang.String name) {
        this.name = name;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof DocumentBlob)) return false;
        DocumentBlob other = (DocumentBlob) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = true && 
            ((this.blob==null && other.getBlob()==null) || 
             (this.blob!=null &&
              java.util.Arrays.equals(this.blob, other.getBlob()))) &&
            ((this.encoding==null && other.getEncoding()==null) || 
             (this.encoding!=null &&
              this.encoding.equals(other.getEncoding()))) &&
            ((this.extensions==null && other.getExtensions()==null) || 
             (this.extensions!=null &&
              java.util.Arrays.equals(this.extensions, other.getExtensions()))) &&
            ((this.mimeType==null && other.getMimeType()==null) || 
             (this.mimeType!=null &&
              this.mimeType.equals(other.getMimeType()))) &&
            ((this.name==null && other.getName()==null) || 
             (this.name!=null &&
              this.name.equals(other.getName())));
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
        if (getBlob() != null) {
            for (int i=0;
                 i<java.lang.reflect.Array.getLength(getBlob());
                 i++) {
                java.lang.Object obj = java.lang.reflect.Array.get(getBlob(), i);
                if (obj != null &&
                    !obj.getClass().isArray()) {
                    _hashCode += obj.hashCode();
                }
            }
        }
        if (getEncoding() != null) {
            _hashCode += getEncoding().hashCode();
        }
        if (getExtensions() != null) {
            for (int i=0;
                 i<java.lang.reflect.Array.getLength(getExtensions());
                 i++) {
                java.lang.Object obj = java.lang.reflect.Array.get(getExtensions(), i);
                if (obj != null &&
                    !obj.getClass().isArray()) {
                    _hashCode += obj.hashCode();
                }
            }
        }
        if (getMimeType() != null) {
            _hashCode += getMimeType().hashCode();
        }
        if (getName() != null) {
            _hashCode += getName().hashCode();
        }
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(DocumentBlob.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://ws.api.platform.ecm.nuxeo.org/jaws", "DocumentBlob"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("blob");
        elemField.setXmlName(new javax.xml.namespace.QName("http://ws.api.platform.ecm.nuxeo.org/jaws", "blob"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "base64Binary"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("encoding");
        elemField.setXmlName(new javax.xml.namespace.QName("http://ws.api.platform.ecm.nuxeo.org/jaws", "encoding"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("extensions");
        elemField.setXmlName(new javax.xml.namespace.QName("http://ws.api.platform.ecm.nuxeo.org/jaws", "extensions"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(true);
        elemField.setMaxOccursUnbounded(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("mimeType");
        elemField.setXmlName(new javax.xml.namespace.QName("http://ws.api.platform.ecm.nuxeo.org/jaws", "mimeType"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("name");
        elemField.setXmlName(new javax.xml.namespace.QName("http://ws.api.platform.ecm.nuxeo.org/jaws", "name"));
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
