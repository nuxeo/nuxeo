import org.nuxeo.runtime.api.Framework;
import org.nuxeo.ecm.platform.usermanager.UserManager;

def main () {
    query = Request.getParameter("query");
    userManager = Framework.getService(UserManager.class)

    results = null
    if (query) {
        results = userManager.searchPrincipals(query)
    }
    Context.render("/users/users.ftl", ['users': results])
}

main()
