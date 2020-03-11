
var root = Repository.GetDocument(null, {
    "value": "/"
});

var newDoc = Document.Create(root, {
        "type": "File",
        "name": "newDoc" + t,
        "properties": {
            "dc:title": "New Title" + t,
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
