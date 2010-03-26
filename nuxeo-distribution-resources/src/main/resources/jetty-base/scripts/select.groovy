/*
Perform a search given a NXQL query

select query:string

Perform a search given a NXQL query.
The query result is printed out as a list of uid and document paths.

Example:
 * fulltext search:
   select * from document where ecm:fulltext like '%test%'
 * List versions
   select * from Document where ecm:isCheckedInVersion=1
 * List proxies
   select * from Document where ecm:isProxy=1
 * List document with deleted lifecycle
   select * from Document where ecm:currentLifeCycleState = 'deleted'

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
int row = 0;
for (doc in docs) {
  row  = row + 1;
  label = doc.getRef().reference() + " " + doc.getPathAsString();
  if (doc.isVersion()) {
    label = label + " [version ${doc.getContextData('version.label')}]";
  }
  if (doc.isProxy()) {
    label = label + " [proxy]";
  }
  if (doc.isLocked()) {
    label = label + " [locked]";
  }
  out.println(label);
}
out.println("(${row} rows)");
