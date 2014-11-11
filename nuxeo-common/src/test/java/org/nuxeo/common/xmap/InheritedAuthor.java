package org.nuxeo.common.xmap;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * @author <a href="mailto:akervern@nuxeo.com">Arnaud Kervern</a>
 */
@XObject(value = "inheritedAuthor", order = { "item1", "item2" })
public class InheritedAuthor extends Author {
    @XNode("notInherited")
    public String notInherited;

    @XNode("@id")
    public String inheritedId;
}
