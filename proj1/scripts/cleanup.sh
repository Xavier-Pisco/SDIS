#! /usr/bin/bash

# Placeholder for clean-up script
# To be executed in the root of the build tree
# Requires at most one argument: the peer id
# Cleans the directory tree for storing
#  both the chunks and the restored files of
#  either a single peer, in which case you may or not use the argument
#    or for all peers, in which case you


# Check number input arguments
argc=$#

if ((argc != 1 ))
then
	echo "Usage: $0 [<peer_id>]]"
	echo "   or: $0 -a"
	exit 1
fi

if [ ! -d peers ]
then
	cd .. # Execute the script on the scripts or on the src folder
fi
if [ ! -d peers ]
then
	cd .. # Execute the script on the build folder
fi

if [ ! -d peers ]
then
	echo "Execute this script from scripts, src, build or proj1 folders"
else
	if (($1 == "-a"))
	then
		rm -rf peers/*
		echo "Removed all peers"
	else
		if [ -d peers/$1 ]
		then
			rm -rf peers/$1
			echo "Removed peer $1"
		else
			echo "Peer $1 has no directory"
		fi
	fi
fi

# Clean the directory tree for storing files
# For a crash course on shell commands check for example:
# Command line basi commands from GitLab Docs':	https://docs.gitlab.com/ee/gitlab-basics/command-line-commands.html
# For shell scripting try out the following tutorials of the Linux Documentation Project
# Bash Guide for Beginners: https://tldp.org/LDP/Bash-Beginners-Guide/html/index.html
# Advanced Bash Scripting: https://tldp.org/LDP/abs/html/

