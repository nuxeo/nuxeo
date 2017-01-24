
var rootDoc = Repository.GetDocument(null, {
    "value": "/"
});

Context.PushDocument(rootDoc,{});

try {
	var restoredRoot = Context.PopDocument(null,{});

	if (restoredRoot==null) {
	  print("KO");
	} else {
	  print("OK");
	}
} catch (err) {
   print(err);
}


