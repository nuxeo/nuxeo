package org.nuxeo.ecm.platform.content.template.service;

public class TemplateItemEditor {
    
    public TemplateItemEditor(TemplateItemDescriptor desc) {
        this.descriptor = desc;
    }
    
    protected TemplateItemDescriptor descriptor;
    
    public void setTypeName(String value){
        descriptor.typeName = value;
    }
    
    public void setId(String value) {
        descriptor.id = value;
    }
    
    public void setTitle(String value) {
        descriptor.title = value;
    }
    
    public void setPath(String value) {
        descriptor.path = value;
    }
    
    public void setDescription(String value) {
        descriptor.description = value;
    }
    
    public PropertyEditor addProperty() {
        PropertyDescriptor edited = new PropertyDescriptor();
        descriptor.properties.add(edited);
        return new PropertyEditor(edited);
    }
    
}
