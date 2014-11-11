#!/bin/bash

PS="ps"
GREP="grep"
SERVERNAME="nxserver"
ECHO="echo"
SLEEP="sleep"
NOHUP="nohup"
AWK="awk"
TERM="kill -15"
KILL="kill -9"

function print_usage {
  echo "Usage: ./nxserverctl.sh start | stop | status | pid";
}


function server_pid {
  PID=$( ${PS} aux | ${GREP} "java" | ${GREP} "nuxeo-runtime-launcher" | ${AWK} '{print $2}' )
}

function check_server {
   PID=$( ${PS} aux | ${GREP} "java" | ${GREP} "nuxeo-runtime-launcher" | ${AWK} '{print $2}' )
   if [ "${PID}" = "" ]; then
      return 1
   else
      return 0
   fi
}


function server_status {
   check_server
   if [ "$?" -eq "0" ]; then
      server_pid
      if [ "${ACTION}" = "start" ]; then
         ${ECHO} "${SERVERNAME} has been started: ${PID}"
      else
         ${ECHO} "${SERVERNAME} is started: ${PID}"
      fi
   else
      ${ECHO} "${SERVERNAME} is stopped"
   fi
}


function stop_server {
  check_server
  if [ "$?" -eq "1" ]; then
    print_error "${SERVERNAME} is already stopped" && exit 1
  fi
  ${TERM} ${PID}
  ${SLEEP} 5
  check_server
  if [ "$?" -eq "1" ]; then
    ${ECHO} "${SERVERNAME} has been shutdown"
  else
    ${KILL} ${PID}
    ${SLEEP} 5
    check_server
    if [ "$?" -eq "1" ]; then
      ${ECHO} "${SERVERNAME} has been killed"
    else
      ${ECHO} "Failed to shutdown server. PID: ${PID}"
    fi
  fi
}

function start_server {
  check_server
  if [ "$?" -eq "0" ]; then
    print_error "${SERVERNAME} is already started" && exit 1
  fi
  ${NOHUP} ./nxserver.sh >${SERVERNAME}.out 2>&1 &
  ${SLEEP} 5
  server_status
}

function print_error {
  ${ECHO} "Error: $@" >&2
}

ACTION="$1"
case ${ACTION} in
     "start")  start_server ;;
     "stop")  stop_server    ;;
     "status")  server_status ;;
     "pid") server_pid ; echo ${PID}  ;;
     *  )  print_usage && exit 1   ;;
esac

