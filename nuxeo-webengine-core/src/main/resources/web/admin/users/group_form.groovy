import org.nuxeo.runtime.api.Framework;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.platform.usermanager.NuxeoPrincipalImpl

def main () {
    username = Context.mapping.getValue("username")
    userManager = Framework.getService(UserManager.class)
    if (username) {
        user = userManager.getPrincipal(username)
        userGroups = user.getGroups()
    } else {
        user = null
        userGroups = null
    }
    allGroups = userManager.getAvailableGroups()
    Context.render("/users/user_form.ftl", ['user': user, 'allGroups': allGroups, 'userGroups': userGroups])
}

main()
