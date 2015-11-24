package info.simplecloud.scimproxy.compliance.test;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import info.simplecloud.core.Group;
import info.simplecloud.core.Resource;
import info.simplecloud.core.User;
import info.simplecloud.scimproxy.compliance.CSP;
import info.simplecloud.scimproxy.compliance.enteties.TestResult;

/**
 * Overriding the default test just to solve resource resolution issue !
 *
 * @author tiry
 *
 */
public class WorkingPostTest extends PostTest {

    public WorkingPostTest(CSP csp, ResourceCache<User> userCache,
            ResourceCache<Group> groupCache) {
        super(csp, userCache, groupCache);
    }

    protected User getUser() {
        try {
            InputStream in = this.getClass().getResourceAsStream("/user_full.json");
            String fullUser = org.nuxeo.common.utils.FileUtils.read(in);
            //String fullUser = FileUtils.readFileToString(new File("src/main/resources/user_full.json"));
            return new User(fullUser, Resource.ENCODING_JSON);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public List<TestResult> run() {
        List<TestResult> results = new ArrayList<TestResult>();

        // simple user
        User scimUser = new User();
        Group scimGroup = new Group();

        // full user
        User scimUserFull = getUser();

        long nanoTime = System.nanoTime();
        // user
        scimUser.setUserName("J" + nanoTime);
        results.add(create("json", scimUser, false));
        scimUser.setUserName("Js" + nanoTime);
        results.add(create("json", scimUser, true));
        scimUserFull.setUserName("Jso" + nanoTime);
        results.add(create("json", scimUserFull, false));

        // group
        scimGroup.setDisplayName("ScimGroupJson");
        results.add(create("json", scimGroup, false));

        // run same tests but now with XML
        if (csp.getSpc().hasXmlDataFormat()) {
            // user
            scimUser.setUserName("X" + nanoTime);
            results.add(create("xml", scimUser, false));
            scimUser.setUserName("Xm" + nanoTime);
            results.add(create("xml", scimUser, true));
            scimUserFull.setUserName("Xml" + nanoTime);
            results.add(create("xml", scimUserFull, false));

            // group
            scimGroup.setDisplayName("ScimGroupXml");
            results.add(create("xml", scimGroup, false));
        }

        return results;
    }

}
