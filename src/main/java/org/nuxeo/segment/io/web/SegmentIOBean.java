package org.nuxeo.segment.io.web;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.contexts.Contexts;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.segment.io.SegmentIO;

@Name("segmentIOActions")
@Scope(ScopeType.EVENT)
@Install(precedence = Install.FRAMEWORK)
public class SegmentIOBean {

    @In(create=true)
    SegmentIO segmentIO;

    @In(create=true)
    NuxeoPrincipal currentNuxeoPrincipal;

    protected static final String SEGMENTIO_FLAG = "segment.io.identify.flag";

    public String getWriteKey() {
        return segmentIO.getWriteKey();
    }

    public boolean isNeedIdentfy() {
        if (currentNuxeoPrincipal==null) {
            return false;
        }

        if (currentNuxeoPrincipal.isAnonymous()) {
            return false;
        }

        Object flag = Contexts.getSessionContext().get(SEGMENTIO_FLAG);
        if (flag == null) {
            Contexts.getSessionContext().set(SEGMENTIO_FLAG, true);
            return true;
        }
        return false;
    }

}
