package org.nuxeo.segment.io;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

@XObject("segmentio")
public class SegmentIOConfig {

    @XNode("writeKey")
    String writeKey;

}
