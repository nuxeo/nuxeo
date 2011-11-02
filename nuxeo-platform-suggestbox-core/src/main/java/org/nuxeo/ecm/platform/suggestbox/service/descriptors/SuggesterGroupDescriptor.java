package org.nuxeo.ecm.platform.suggestbox.service.descriptors;

import java.util.ArrayList;
import java.util.List;

public class SuggesterGroupDescriptor {

    protected String name = "default";

    public String getName() {
        return name ;
    }

    public List<String> getSuggesters() {
        List<String> suggesters = new ArrayList<String>();
        // TODO: 
        return suggesters;
    }

}
