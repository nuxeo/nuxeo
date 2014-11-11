#!/bin/sh
convert $1 -crop 255x255  +repage  /tmp/tiles_%02d.jpg

