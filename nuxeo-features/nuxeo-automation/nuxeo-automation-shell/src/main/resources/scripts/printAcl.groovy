
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;


def doc = null;
def aclname = Context["acl"];
def ref = Context["ref"];
if (ref.startsWith("/")) {
  doc = Session.getDocument(new PathRef(ref));
} else {
  doc = Session.getDocument(new IdRef(ref));
}
def acp = doc.getACP();
def result = null;
if (aclname != null) {
  def acl = acp.getACL(aclname);
  if (acl == null) {
    result = "No Such ACL: ${aclname}. Available ACLS: ";
    for (a in acp.getACLs()) {
      result+=a.getName()+" ";
    }
    return result;
  }
  result = "{bold}${aclname}{bold}\n";
  for (ace in acl) {
    result += "\t${ace}\n";
  }
} else {
  result = "";
  for (acl in acp.getACLs()) {
    result += "{bold}${acl.name}{bold}\n";
    for (ace in acl) {
      result += "\t${ace}\n";
    }
  }
}

return result;
