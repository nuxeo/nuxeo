package org.nuxeo.ecm.core.cache;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.core.api.NuxeoException;

@XObject("cache")
public class InMemoryCacheDescriptor extends CacheDescriptor {

    {
        type = "inmemory";
    }

    @XNode(value="concurrencyLevel",context="nuxeo.cache.concurrencylevel")
    int concurrencyLevel = -1;

    @XNode(value="maxSize",context="nuxeo.cache.maxsize")
    int maxSize = -1;

    @Override
    protected void injectOption(String name, String option) {
        if ("concurrencyLevel".equals(name)) {
            concurrencyLevel = Integer.valueOf(option);
        } else if ("maxSize".equals(name)) {
            maxSize = Integer.valueOf(option);
        } else {
            throw new NuxeoException("Unsupported option " + name);
        }
    }

    @Override
    public String toString() {
        return super.toString().concat(",maxSize="+maxSize).concat(",concurrencyLevel"+concurrencyLevel);
    }

}
