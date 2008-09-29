package org.nuxeo.ecm.core.search.api.indexingwrapper;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

@XObject(value = "indexingWrapper")
public class DocumentModelIndexingWrapperDescriptor {

    @XNode("@name")
    private String name;

    @XNode("@enabled")
    private boolean enabled = true;

    @XNode("typeName")
    private String typeName;

    @XNode("class")
    private Class adapterClass;

    public Class getAdapterClass() {
        return adapterClass;
    }

    public void setAdapterClass(Class adapterClass) {
        this.adapterClass = adapterClass;
    }

    public boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public DocumentIndexingWrapperFactory getNewInstance() throws InstantiationException,
            IllegalAccessException {
        return (DocumentIndexingWrapperFactory) adapterClass.newInstance();
    }

}
