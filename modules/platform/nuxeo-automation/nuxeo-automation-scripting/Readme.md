## About

This module is a work in progress for allowing to use JavaScript

 - as an Automation based DSL to write custom extensions
 - to create "Automation chains"

## Motivations

The initial idea comes from the following constations :

 - Automation Operations are a good High Level API : people succeed is doing amazing stuffs with it
 - Automation Chain control flow is too primitive
     - loops are complex
     - conditions are complex
     - reusing "code segment" forces using several chains and makes maintenability more complex

So, the idea is to use a simple scripting language to manage the control flow and give accees to the Automation API.

## Example :

Here is a example of an Automation Script :

    var root = Document.Fetch(null, {
        "value": "/"
    });

    var newDocs = [];
    for (var i = 1; i < 10; i++) {

        var newDoc = Document.Create(root, {
            "type": "File",
            "name": "newDoc" + i,
            "properties": {
                "dc:title": "New Title" + i,
                "dc:source": "JavaScript",
                "dc:subjects": ["from", "javascript"]
            }
        });
        var lastUUIDDigit = parseInt(newDoc.getId().slice(-1));
        if (lastUUIDDigit % 2 == 0) {
            newDoc = Document.Update(newDoc, {
                "properties": {
                    "dc:nature": "even"
                }
            });
        } else {
            newDoc = Document.Update(newDoc, {
                "properties": {
                    "dc:nature": "odd"
                }
            });
        }
        newDocs.push(newDoc);
    }

    var evenDocs = Document.Query(null, {
        "query": "select * from Document where dc:nature='even'"
    });

    println("Created " + evenDocs.size() + " even Documents");


Here is an exemple of an "Automation Chain" defined via a script


      <scriptedOperation id="Scripting.AddFacetInSubTree">
         <inputType>Document</inputType>
         <outputType>Documents</outputType>
         <param name="facet" type="string"/>
         <param name="type" type="string"/>

         <script><![CDATA[
           function run(ctx, input, params) {

             var query = "select * from " + params.type + " where ecm:path startswith '";
             query = query + input.getPathAsString();
             query = query + "'";

             //println("query = " + query);
             var subDocs = Document.Query(null, {
			"query": query
		   });

		   //println("Query run with " + subDocs.size() + " result");

		   var updated = [];
		   for (var i = 0; i < subDocs.size(); i++) {
		      var doc = subDocs.get(i);
		      if (!doc.hasFacet(params.facet)) {
		         doc.addFacet(params.facet);
		         updated.push(Document.Save(doc,{}));
		      }
		   }
             return updated;
           }
           ]]>
         </script>
      </scriptedOperation>
