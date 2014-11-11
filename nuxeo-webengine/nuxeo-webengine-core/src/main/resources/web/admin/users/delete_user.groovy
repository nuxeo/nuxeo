import org.nuxeo.runtime.api.Framework;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.platform.usermanager.NuxeoPrincipalImpl

def main () {
    username = Request.getParameter("username");
    userManager = Framework.getService(UserManager.class)
    if (username) {
        user = userManager.getPrincipal(username)
        if (user != null) {
            userManager.deletePrincipal(user)
        }
    }
    Context.redirect(appPath + "/users")
}

main()
