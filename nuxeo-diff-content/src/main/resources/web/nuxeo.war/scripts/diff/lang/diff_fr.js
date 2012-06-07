function previousLinkLabel() {
	return "Précédent";
}

function nextLinkLabel() {
	return "Suivant";
}

function translateDiffId(diffId) {
	var lastIndexOfDash = diffId.lastIndexOf("-"); 
	if (lastIndexOfDash > -1) {
		var diffType = diffId.substring(0, lastIndexOfDash);
		var diffCount = parseInt(diffId.substring(lastIndexOfDash + 1, diffId.length1)) + 1;
		if (diffType == "removed-diff") {
			return "Suppression n° " + diffCount; 
		} else if (diffType == "added-diff") {
			return "Ajout n° " + diffCount;
		} else if (diffType == "changed-diff") {
			return "Modification n° " + diffCount;
		}
	}
	return diffId;
}
