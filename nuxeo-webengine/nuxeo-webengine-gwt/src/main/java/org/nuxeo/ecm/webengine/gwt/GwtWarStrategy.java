package org.nuxeo.ecm.webengine.gwt;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.webengine.gwt.GwtResolver.Strategy;

@XObject("strategy")
public class GwtWarStrategy extends GwtWarLocation {

    public Strategy strategy = GwtResolver.ROOT_RESOLVER_STRATEGY;

    @XNode()
    void setResolver(Class<? extends Strategy> type) {
        try {
            strategy = type.newInstance();
        } catch (ReflectiveOperationException cause) {
            throw new NuxeoException("Cannot load " + type, cause);
        }
    }

    @Override
    public String toString() {
        return "GWT War Strategy Resolver [" + name + "," + strategy.source() + "]";
    }

}
