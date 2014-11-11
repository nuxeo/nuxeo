/*

Publish the current document into the target section

publish|pub [docToPublish:doc] [section:doc]


This command publish the current document into the given section. 
Relative or absolute paths may be used to specify the target section.
*/

import org.nuxeo.ecm.core.api.*;
import org.nuxeo.common.utils.*;

def params = cmdLine.getParameters();
if (params.length != 2) {
  out.println("Syntax Error: the publish command take exactly two parameters - the target section path");
  return;
}

def session = ctx.getRepositoryInstance();
def docPath = params[0];
def sectionPath = params[1];
def parent = ctx.fetchDocument();

if (!docPath.startsWith("/")) {
  docPath = parent.getPath().append(docPath).toString();
}
doc = session.getDocument(new PathRef(docPath));

out.println("Publishing ${doc.getPathAsString()} into ${sectionPath}");

if (!doc.hasFacet("Versionable")) {
  out.println("Error: Cannot publish the document. It is not Versionable");
  return;  
}

DocumentModel section = session.getDocument(new PathRef(sectionPath));
if (!"Section".equals(section.getType())) {
  out.println("Error: Cannot publish the document. The destination is not a section");
  return;  
}

session.publishDocument(doc, section);
session.save();
out.println("Done");
