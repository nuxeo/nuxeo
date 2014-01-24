package org.nuxeo.segment.io;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;

@XObject("mapper")
public class SegmentIOMapper {

    @XNode("@name")
    String name;

    @XNode("@targetAPI")
    String target = "track";

    @XNodeList(value = "events/event", type = ArrayList.class, componentType = String.class)
    List<String> events;

    @XNode("mvel")
    String mvelMapping;

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof SegmentIOMapper) {
            return name.equals(((SegmentIOMapper)obj).name);
        }
        return super.equals(obj);
    }

}
