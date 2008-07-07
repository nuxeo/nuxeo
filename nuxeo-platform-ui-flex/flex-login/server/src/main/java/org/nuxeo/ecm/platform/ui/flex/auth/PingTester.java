package org.nuxeo.ecm.platform.ui.flex.auth;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.remoting.WebRemote;

@Name("authPingTester")
@Scope(ScopeType.STATELESS)
public class PingTester {

    @WebRemote
    public String ping()
    {
        return "pong";
    }
}
