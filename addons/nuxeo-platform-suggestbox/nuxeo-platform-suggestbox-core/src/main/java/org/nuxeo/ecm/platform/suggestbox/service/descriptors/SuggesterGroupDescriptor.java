package org.nuxeo.ecm.platform.suggestbox.service.descriptors;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;

@XObject("suggesterGroup")
public class SuggesterGroupDescriptor implements Cloneable {

    @XNode("@name")
    protected String name = "default";

    @XNodeList(value = "suggesters/suggesterName", type = ArrayList.class, componentType = String.class)
    List<String> suggesters;
    
    // TODO make it possible to do incremental contribution with
    // <suggesterName appendAfter="otherSuggesterName">suggesterNameToAppend</suggesterName>
    // or
    // <suggesterName remove="true">suggesterNameToRemove</suggesterName>

    public String getName() {
        return name;
    }

    public List<String> getSuggesters() {
        return suggesters;
    }

    /*
     * Override the Object.clone to make it public
     */
    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}
