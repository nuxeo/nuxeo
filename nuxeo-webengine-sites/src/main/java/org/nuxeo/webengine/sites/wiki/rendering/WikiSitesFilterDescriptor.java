package org.nuxeo.webengine.sites.wiki.rendering;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * @author <a href="mailto:cbaican@nuxeo.com">Catalin Baican</a>
 *
 */
@XObject("filter")
public class WikiSitesFilterDescriptor {

    @XNode("@pattern")
    public String pattern;

    @XNode("@replacement")
    public String replacement;

    @XNode("@class")
    public String clazz;

}
