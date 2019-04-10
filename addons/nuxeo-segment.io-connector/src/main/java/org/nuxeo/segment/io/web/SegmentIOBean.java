package org.nuxeo.segment.io.web;

import java.io.Serializable;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.contexts.Contexts;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.segment.io.SegmentIO;

@Name("segmentIOActions")
@Scope(ScopeType.EVENT)
@Install(precedence = Install.FRAMEWORK)
public class SegmentIOBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @In(create=true)
    NuxeoPrincipal currentNuxeoPrincipal;

    protected static final String SEGMENTIO_FLAG = "segment.io.identify.flag";

    public String getWriteKey() {
        return Framework.getLocalService(SegmentIO.class).getWriteKey();
    }

    public boolean needsIdentify() {
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
