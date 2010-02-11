package org.nuxeo.ecm.platform.content.template.service;

public class ACEEditor {
    
    public ACEEditor(ACEDescriptor desc) {
        this.descriptor = desc;
    }
    
    ACEDescriptor descriptor;
    
    public void setGranted(boolean value) {
        descriptor.granted = value;
    }
    
    public void setPrincipal(String value) {
        descriptor.principal = value;
    }
    
    public void setPermission(String value) {
        descriptor.permission = value;
    }

}
