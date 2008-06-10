import org.nuxeo.runtime.api.Framework;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.platform.usermanager.NuxeoPrincipalImpl

def main () {
    attributes = Context.pathInfo.attributes
    if (attributes) {
        username = attributes.getValue("username")
    } else {
        username = null
    }
    userManager = Framework.getService(UserManager.class)
    if (username) {
        user = userManager.getPrincipal(username)
    } else {
        user = null
    }
    allGroups = userManager.getAvailableGroups()
    Context.render("/users/user_form.ftl", ['user': user, 'allGroups': allGroups])
}

main()
