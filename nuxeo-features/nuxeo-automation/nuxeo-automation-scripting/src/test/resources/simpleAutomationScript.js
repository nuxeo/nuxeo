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

var evenDocs = Repository.Query(null, {
    "query": "select * from Document where dc:nature='even'"
});
if(evenDocs.size()>0) {
    print("Created even Documents");
}