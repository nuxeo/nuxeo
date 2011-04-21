var base = 1024;

function getReadableSpeedString(speedInKBytesPerSec)
{
	var speed = speedInKBytesPerSec;
	speed = Math.round(speed * 10) / 10;
	if (speed < base) {
		return speed + "KB/s";
	}

	speed /= base;
	speed = Math.round(speed * 10) / 10;
	if (speed < base) {
		return speed + "MB/s";
	}

	return speedInBytesPerSec + "B/s";				
}

function getReadableFileSizeString(fileSizeInBytes)
{
	var fileSize = fileSizeInBytes;
	if (fileSize < base) {
		return fileSize + "B";
	}

	fileSize /= base;
	fileSize = Math.round(fileSize);
	if (fileSize < base) {
		return fileSize + "KB";
	}
	
	fileSize /= base;
	fileSize = Math.round(fileSize * 10) / 10;
	if (fileSize < base) {
		return fileSize + "MB";
	}

	return fileSizeInBytes + "B";
}

function getReadableDurationString(duration)
{
	var elapsed = duration;

	var minutes, seconds;

	seconds = Math.floor(elapsed / 1000);
	minutes = Math.floor((seconds / 60));
	seconds = seconds - (minutes * 60);

	var str = "";
	if (minutes>0)
		str += minutes + "m";

	str += seconds + "s";
	return str;
}