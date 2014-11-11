import org.nuxeo.runtime.api.Framework;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.platform.usermanager.NuxeoPrincipalImpl

def main () {
    username = Request.getParameter("username");
    userManager = Framework.getService(UserManager.class)
    if (username) {
        group = userManager.getGroup(username)
        if (group != null) {
            userManager.deleteGroup(group)
        }
    }
    Context.redirect(appPath + "/users")
}

main()
