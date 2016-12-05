function inject() { 
var root = Repository.GetDocument(null, {
    "value": "/"
});

for (var i = 1; i <= 10; i++) {
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
    Document.Update(newDoc, {
        "properties": {
            "dc:nature": "injected by simple script"
        }
    });
}

Document.SaveSession(null, {}); // flush for being able to query
}

inject();


Repository.Query(null, {
    "query": "select * from Document where dc:nature='injected by simple script'"
});