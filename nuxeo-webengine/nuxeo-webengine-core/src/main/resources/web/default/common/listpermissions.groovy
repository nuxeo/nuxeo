import org.nuxeo.runtime.api.Framework
import org.nuxeo.ecm.platform.usermanager.UserManager
import org.nuxeo.ecm.core.api.security.ACE
import org.nuxeo.ecm.core.api.security.ACL
import org.nuxeo.ecm.core.api.security.ACP


class Permission {
    String name
    boolean granted
    String permission

    Permission(String name, String permission, boolean granted) {
        this.name = name
        this.permission = permission
        this.granted = granted
    }
}

acp = Context.coreSession.getACP(Context.targetDocument.ref);

permissions = []
for (acl in acp.getACLs()) {
    for (ace in acl.getACEs()) {
        permissions.add(new Permission(ace.username, ace.permission, ace.isGranted()))
    }
}

return permissions