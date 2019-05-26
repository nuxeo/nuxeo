function scroll(x, y) {
	window.scrollTo(x, y);
}
function getXY(event) {
	alert(getX(event) + "," + getY(event));
}
function getX(event) {
	return event.clientX - event.target.offsetTop;
}

function getY(event) {
	return event.clientY - event.target.offsetLeft;
}