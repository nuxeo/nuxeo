import org.nuxeo.runtime.api.Framework;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.platform.usermanager.NuxeoPrincipalImpl

def main () {
    username = Context.getProperty("username")
    userManager = Framework.getService(UserManager.class)
    if (username) {
        user = userManager.getGroup(username)
        if (user != null) {
          usersGroup = user.getMemberUsers()
        }
    } else {
        user = null
        usersGroup = null
    }
    allGroups = userManager.getAvailableGroups()
    Context.render("/users/group_form.ftl", ['group': user, 'allGroups': allGroups, 'usersGroup': usersGroup])
}

main()
