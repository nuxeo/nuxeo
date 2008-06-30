import org.nuxeo.runtime.api.Framework
import org.nuxeo.ecm.platform.usermanager.UserManager
import org.nuxeo.ecm.core.api.security.ACE
import org.nuxeo.ecm.core.api.security.ACL
import org.nuxeo.ecm.core.api.security.ACP
import org.nuxeo.ecm.core.api.security.SecurityConstants
import org.nuxeo.ecm.core.api.security.impl.ACLImpl
import org.nuxeo.ecm.core.api.security.impl.ACPImpl

action = Request.getParameter("action")
permission = Request.getParameter("permission")
username = Request.getParameter("user")


userManager = Framework.getService(UserManager.class)
user = userManager.getPrincipal(username)
if (!user) {
    user = userManager.getGroup(username)
}

if (user) {
    acp = new ACPImpl()
    acl = new ACLImpl(ACL.LOCAL_ACL)
    acp.addACL(acl)
    granted = action.equals("grant")
    ACE ace = new ACE(username, permission, granted)
    acl.add(ace)
    Context.coreSession.setACP(Context.targetDocument.ref, acp, false)
    Context.coreSession.save()
    Context.redirect(Context.targetObjectUrlPath)
}

