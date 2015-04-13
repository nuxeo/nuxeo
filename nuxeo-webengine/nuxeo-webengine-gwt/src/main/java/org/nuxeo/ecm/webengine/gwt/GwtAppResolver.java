package org.nuxeo.ecm.webengine.gwt;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URI;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.core.api.NuxeoException;

@XObject("resolver")
public class GwtAppResolver {

    @XNode("@application")
    public String name;

    public Strategy strategy = GwtResolver.ROOT_RESOLVER_STRATEGY;

    @XNode("strategy")
    void setResolver(Class<? extends Strategy> type) {
        try {
            strategy = type.newInstance();
        } catch (ReflectiveOperationException cause) {
            throw new NuxeoException("Cannot load " + type, cause);
        }
    }

    public interface Strategy {

        URI source();

        File resolve(String path) throws FileNotFoundException;
    }

    @Override
    public String toString() {
        return "GWT resolver [" + name + "," + strategy.source() + "]";
    }

}
