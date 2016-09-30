import bpy, json, os, sys, time, bmesh
from bpy import context
from bpy_extras.object_utils import world_to_camera_view
from copy import copy
from math import pi, cos, sin, degrees, radians
from mathutils import Vector
import argparse


def spherical_coords(s):
    try:
        if s == '':
            return 0, 0
        zenith, azimuth = map(int, s.split(','))
        return zenith, azimuth
    except:
        raise argparse.ArgumentTypeError('Coordinates must be zenith,azimuth')


def dimensions(d):
    try:
        if d == '':
            return 0, 0
        width, height = map(int, d.split('x'))
        return width, height
    except:
        raise argparse.ArgumenTypeError('Dimensions must be width,height')


def params_filled(params):
    return params is not None and len(params) > 0 and params[0] != ''

parser = argparse.ArgumentParser(description='Blender pipeline.')
parser.add_argument('-op', '--operators', dest='operators', nargs='*',
                    choices=['import', 'info', 'lod', 'render', 'convert'],
                    help='a list of operators to run in the pipeline (options: import,lod,render,convert)')
parser.add_argument('-i', '--input', dest='input',
                    help='path for the input file')
parser.add_argument('-o', '--outdir', dest='outdir',
                    help='path for output dir')
parser.add_argument('-li', '--lodids', dest='lodids', nargs='*',
                    help='a list of ids to use on lod')
parser.add_argument('-pp', '--percpoly', dest='percpoly', nargs='*',
                    help='a list of polygon percentage values to use on the lod operator (options: 0-100)')
parser.add_argument('-mp', '--maxpoly', dest='maxpoly', nargs='*',
                    help='a list of max polygon values to use on the lod operator')
parser.add_argument('-ri', '--renderids', dest='renderids', nargs='*',
                    help='a list of ids to use on render')
parser.add_argument('-d', '--dimensions', help='list of dimensions for render',
                    dest='dimensions', type=dimensions, nargs='*')
parser.add_argument('-c', '--coords', help='list of spherical coordinates for render',
                    dest='coords', type=spherical_coords, nargs='*')

args_to_parse = sys.argv[sys.argv.index('--') + 1:]
print(args_to_parse)
args = parser.parse_args(args_to_parse)
print('operators:')
print(args)
if args.operators is None:
    sys.exit()

bpy.ops.wm.addon_enable(module="materials_utils")
base_path = os.path.dirname(os.path.abspath(__file__)) + '/pipeline/'
perc_poly = 100
max_poly = None
lod_id = 'default'
lod_success = True

if params_filled(args.lodids):
    lod_args = {'i': args.lodids, 'pp': [], 'mp': []}
    for i in range(0, len(args.lodids)):
        if not params_filled(args.percpoly) or args.percpoly[i] == 'None' or args.percpoly[i] == 'null':
            lod_args['pp'].append(None)
        else:
            lod_args['pp'].append(int(args.percpoly[i]))
        if not params_filled(args.maxpoly) or args.maxpoly[i] == 'None' or args.maxpoly[i] == 'null':
            lod_args['mp'].append(None)
        else:
            lod_args['mp'].append(int(args.maxpoly[i]))

for operator in args.operators:
    if operator == 'lod':
        lod_id = lod_args['i'].pop(0)
        perc_poly = lod_args['pp'].pop(0)
        max_poly = lod_args['mp'].pop(0)
    if operator == 'render':
        coords = args.coords.pop(0)
        dim = args.dimensions.pop(0)
        id = args.renderids.pop(0)
    filename = base_path + operator + '.py'
    exec (compile(open(filename).read(), filename, 'exec'))
