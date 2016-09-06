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
        width, height = map(int, d.split(','))
        return width, height
    except:
        raise argparse.ArgumenTypeError('Dimensions must be width,height')


def params_filled(params):
    return params is not None and len(params) > 0 and params[0] != ''

parser = argparse.ArgumentParser(description='Blender pipeline.')
parser.add_argument('-i', '--input', dest='input',
                    help='path for the input file')
parser.add_argument('-o', '--outdir', dest='outdir',
                    help='path for output dir')
parser.add_argument('-op', '--operators', dest='operators', nargs='*', choices=['import', 'lod', 'render', 'convert'],
                    help='a list of operators to run in the pipeline (options: import,lod,render,convert)')
parser.add_argument('-li', '--lodids', dest='lodids', nargs='*',
                    help='a list of ids to use on lod')
parser.add_argument('-l', '--lods', dest='lods', nargs='*',
                    help='a list of level of detail values to use on the lod operator (options: 0-100)')
parser.add_argument('-mp', '--maxpolys', dest='maxpolys', nargs='*',
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

base_path = os.path.dirname(os.path.abspath(__file__)) + '/pipeline/'
lod = 100
current_lod = 1.0

if params_filled(args.lodids):
    lod_args = {'i': args.lodids, 'l': [], 'mp': []}
    for i in range(0, len(args.lodids)):
        if not params_filled(args.lods) or args.lods[i] == 'None' or args.lods[i] == 'null':
            lod_args['l'].append(None)
        else:
            lod_args['l'].append(int(args.lods[i]))
        if not params_filled(args.maxpolys) or args.maxpolys[i] == 'None' or args.maxpolys[i] == 'null':
            lod_args['mp'].append(None)
        else:
            lod_args['mp'].append(int(args.maxpolys[i]))

for operator in args.operators:
    if operator == 'lod':
        lodid = lod_args['i'].pop(0)
        lod = lod_args['l'].pop(0)
        max_polygons = lod_args['mp'].pop(0)
    if operator == 'render':
        coords = args.coords.pop(0)
        dim = args.dimensions.pop(0)
        id = args.renderids.pop(0)
    filename = base_path + operator + '.py'
    exec (compile(open(filename).read(), filename, 'exec'))
