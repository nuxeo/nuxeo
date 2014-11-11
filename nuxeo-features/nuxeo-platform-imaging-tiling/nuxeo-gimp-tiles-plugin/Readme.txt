
Requirements :
--------------
Gimp 2 with python extensions.

How to install :
----------------
Copy the python script in $HOME/.gimp-2.x/plug-ins.
$HOME must be the home of the user that will execute gimp.
Alternatively, you may deploy the script in /usr/lib/gimp/2.x/plug-ins/.

The python script must be executable (chmod +x).

How to run the tile plugin :
----------------------------
You can run it interactively from the Xtns/Nuxeo menu.
Sine this plugin is made to be run in command line, all parameters are very simple, this means that for example the input and output files are set as simple string rather than using the gimp file dialog.

You can also run it via Gimp command line (does not require X).
gimp --no-interface --batch '(python-fu-nx-tiles RUN-NONINTERACTIVE "<input-image-path>" <tile-width> <tile-height> <max-nb-tiles> "<output-directory>" <center_x_tile/-1> <center_y_tile/-1>)' --batch '(gimp-quit 1)'

See the run.sh script in the cmd directory for an example.


Parameters :
------------
<input-image-path> : complete path of the original image 
<tile-width> : width of the generated tiles (pixels)
<tile-height> : height of the generated tiles (pixels)
<max-nb-tiles> : maximum number of tiles in the X axis or on the Y axis
<output-directory> : path of the directory where the tiles are saved
<center_x_tile/-1> : index of the center tiles in the X axis, this is used to do a partial tiles generation (use -1 to generate all)
<center_y_tile/-1> : index of the center tiles in the Y axis, this is used to do a partial tiles generation (use -1 to generate all)


~
