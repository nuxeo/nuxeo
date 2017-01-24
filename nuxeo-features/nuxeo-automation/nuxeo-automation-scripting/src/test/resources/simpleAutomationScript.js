var root = Repository.GetDocument(null, {
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
            "dc:subjects": ["from", "javascript"],
            "dc:nature": "created"
        }
    });
    newDoc = Document.Update(newDoc, {
        "properties": {
            "dc:nature": "updated"
        }
    });
}

var evenDocs = Repository.Query(null, {
    "query": "select * from Document where dc:nature='updated'"
});
if(evenDocs.size()>0) {
    print("Documents Updated");
}