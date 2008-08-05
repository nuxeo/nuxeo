/*

Perform a search given a NXQL query

select query:string


Perform a search given a NXQL query. 
The query result is printed out as a list of document paths.
Example: select * from document where ecm:fulltext like '%test%'"
*/

import org.nuxeo.ecm.core.api.*;
import org.nuxeo.common.utils.*;


def params = cmdLine.getParameters();
if (params.length == 0) {
  out.println(params.length);
  out.println("Syntax Error: the search command take exactly one parameter - the query string");
  return;
}


def docs = ctx.getRepositoryInstance().query("select "+params.join(" "));

for (doc in docs) {  
  if (doc.isVersion()) {
    def proxies = ctx.getRepositoryInstance().query("select * from document where ecm:refFrozenNode='${doc.id}'");
    for (p in proxies) {
      out.println(p.getPathAsString() + " -> " + doc.getPathAsString() + " [version: ${doc.getContextData('version.label')}]");
    }
  } else {
    out.println(doc.getPathAsString());
  }
}

