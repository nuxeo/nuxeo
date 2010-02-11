package org.nuxeo.ecm.platform.content.template.service;

public class FactoryBindingEditor {

    protected FactoryBindingDescriptor descriptor;
    
    public FactoryBindingEditor(FactoryBindingDescriptor desc) {
        this.descriptor = desc;
    }
    
    public void setName(String name){
        descriptor.name = name;
    }
    
    public void setFactoryName(String name) {
        descriptor.factoryName = name;
    }
    
    public void setTargetType(String value) {
        descriptor.targetType = value;
    }
    
    public void setTargetFacet(String value) {
        descriptor.targetFacet = value;
    }
    
    public void putOption(String key, String value) {
        descriptor.options.put(key, value);
    }
    
    public TemplateItemEditor addTemplateItem() {
        TemplateItemDescriptor edited = new TemplateItemDescriptor();
        descriptor.template.add(edited);
        return new TemplateItemEditor(edited);
    }
    
    public ACEEditor addACE() {
        ACEDescriptor edited = new ACEDescriptor();
        descriptor.rootAcl.add(edited);
        return new ACEEditor(edited);
    }
}
