import org.nuxeo.runtime.api.Framework;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.core.api.impl.NuxeoGroupImpl

def main () {
    groupName = Request.getParameter("groupName");
    userManager = Framework.getService(UserManager.class)
    if (groupName) {
        group = new NuxeoGroupImpl(groupName)
        userManager.createGroup(group)
    }
    Context.redirect(appPath + "/users")
}

main()
