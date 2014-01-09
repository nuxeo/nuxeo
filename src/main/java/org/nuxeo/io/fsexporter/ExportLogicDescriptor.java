package org.nuxeo.io.fsexporter;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

@XObject("exportLogic")
public class ExportLogicDescriptor {
    @XNode("@class")
    public Class<? extends FSExporterPlugin> plugin;

}
