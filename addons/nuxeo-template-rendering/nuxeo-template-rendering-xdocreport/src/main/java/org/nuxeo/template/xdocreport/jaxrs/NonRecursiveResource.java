package org.nuxeo.template.xdocreport.jaxrs;

import fr.opensagres.xdocreport.remoting.resources.domain.Resource;

/**
 * Simple wrapper to avoid infinit recursion in JSON marshaling
 * 
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 * 
 */
public class NonRecursiveResource extends Resource {
    public Resource getParent() {
        return null;
    }
}
