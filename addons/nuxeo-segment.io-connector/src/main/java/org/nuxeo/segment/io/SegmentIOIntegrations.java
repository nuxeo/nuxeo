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
@XObject("integrationsConfig")
public class SegmentIOIntegrations {

    @XNodeMap(value = "integrations/integration", key = "@name", type = HashMap.class, componentType = Boolean.class)
    Map<String, Boolean> integrations = new HashMap<String, Boolean>();

}
