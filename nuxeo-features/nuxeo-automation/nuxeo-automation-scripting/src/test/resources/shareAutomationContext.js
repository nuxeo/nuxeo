
var rootDoc = Repository.GetDocument(null, {
    "value": "/"
});

Context.PushDocument(rootDoc,{});

function checkRestore() {
try {
	var restoredRoot = Context.PopDocument(null,{});

	if (restoredRoot==null) {
	  return "KO";
	} else {
	  return "OK";
	}
} catch (err) {
   return "ERR";
}
}

checkRestore();


