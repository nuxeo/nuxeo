import org.nuxeo.runtime.api.Framework;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.platform.usermanager.NuxeoPrincipalImpl

def main () {
    username = Context.getProperty("username")
    userManager = Framework.getService(UserManager.class)
    def userGroups = null
    if (username) {
        user = userManager.getPrincipal(username)
        if (user != null) {
          userGroups = user.getGroups()
        }
    } else {
        user = null
    }
    allGroups = userManager.getAvailableGroups()
    Context.render("/users/user_form.ftl", ['user': user, 'allGroups': allGroups, 'userGroups':userGroups])
}

main()
