#!/bin/sh
gimp --no-interface --batch '(python-fu-nx-tiles RUN-NONINTERACTIVE "/home/tiry/photos/orion.jpg" 255 255 20 "/tmp/" 0 0)' --batch '(gimp-quit 1)'
