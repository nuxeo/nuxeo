package org.nuxeo.apidoc.ext;

import org.nuxeo.apidoc.api.ExtensionInfo;

public interface ExtensionRendererFactory {

    ExtensionRenderer getRendered(ExtensionInfo ei);

}
