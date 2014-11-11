importPackage(java.lang);
importPackage(org.nuxeo.ecm.client);
importPackage(org.nuxeo.ecm.core.api);

var client = NuxeoClient.getInstance();
var repo = client.openRepository();
var it = new DocumentTreeIterator(repo, repo.getRootDocument());
while (it.hasNext()) {
    var doc = it.next();
    println(">> " + doc.getPathAsString());
}
