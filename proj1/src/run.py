import os, getopt, subprocess, signal, sys, time, argparse

version = 1.2
acessPoint = 1000
port = 4000
npeers = 10

children = []

def signal_handler(sig, frame):
	for i in range(len(children)):
		os.kill(children[i][0], signal.SIGTERM)
	os.kill(rmi_pid, signal.SIGTERM)
	sys.exit(0)

signal.signal(signal.SIGINT, signal_handler)

parser = argparse.ArgumentParser()
parser.add_argument("-c", "--compile", action="store_true", help="compiles all .java files")
parser.add_argument("-n", "--number", help="number of peers")
args = parser.parse_args()
if args.compile:
	print("Compiling")
	if (os.system("javac *.java") != 0):
		sys.exit(1);
if args.number:
	npeers = int(args.number)

pid = os.fork()
if pid == 0:
	subprocess.call(["rmiregistry"])
	exit(0)

rmi_pid = pid

time.sleep(0.5)

for i in range(npeers):
	pid = os.fork()
	if pid == 0:
		subprocess.call(["java", "Peer", str(version), str(acessPoint + i), str(acessPoint + i), "230.0.0.1", str(port), "230.0.0.2", str(port), "230.0.0.3", str(port)])
		exit(0)
	children.append([pid, acessPoint + i])

print("All peers runing")
signal.pause()