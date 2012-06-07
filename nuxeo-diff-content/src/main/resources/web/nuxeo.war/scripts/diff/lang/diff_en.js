function previousLinkLabel() {
	return "Previous";
}

function nextLinkLabel() {
	return "Next";
}

function translateDiffId(diffId) {
	var lastIndexOfDash = diffId.lastIndexOf("-"); 
	if (lastIndexOfDash > -1) {
		var diffType = diffId.substring(0, lastIndexOfDash);
		var diffCount = parseInt(diffId.substring(lastIndexOfDash + 1, diffId.length1)) + 1;
		if (diffType == "removed-diff") {
			return "Deletion n° " + diffCount; 
		} else if (diffType == "added-diff") {
			return "Insertion n° " + diffCount;
		} else if (diffType == "changed-diff") {
			return "Change n° " + diffCount;
		}
	}
	return diffId;
}
