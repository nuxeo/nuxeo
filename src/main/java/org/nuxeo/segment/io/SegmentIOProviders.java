package org.nuxeo.segment.io;

import java.util.HashMap;
import java.util.Map;

import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 *
 */
@XObject("providersConfig")
public class SegmentIOProviders {

    @XNodeMap(value = "providers/provider", key = "@name", type = HashMap.class, componentType = Boolean.class)
    Map<String, Boolean> providers = new HashMap<String, Boolean>();

}
