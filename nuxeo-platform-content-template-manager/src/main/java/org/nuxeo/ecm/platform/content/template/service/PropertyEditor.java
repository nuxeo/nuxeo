package org.nuxeo.ecm.platform.content.template.service;

public class PropertyEditor {
    
    PropertyEditor(PropertyDescriptor desc) {
        this.descriptor = desc;
    }
    
    protected PropertyDescriptor descriptor;
    
    public void setXPath(String value) {
        descriptor.xpath = value;
    }
    
    public void setValue(String value) {
        descriptor.value = value;
    }

}
