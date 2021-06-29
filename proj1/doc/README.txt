Group 07

All the scripts should be run from the folder scripts, src or build,
with exception from compile.sh that you can only run from the folder scripts or src.

- compile.sh
Run this script to compile all the code in a build folder inside the src folder.

- cleanup.sh
This script can be run with the id of the peer to clean or with '-a' in order to remove all peers.

- setup.sh
This script is not needed on our project.

- peer.sh
Run this script to initialize a peer. It must have the needed parameters.

- test.sh
Run this script to initialize the TestApp. It must have the needed parameters.
The file chosen to initiate backup must have the path from the test folder.
Example: for the file proj1/test/google20KB.png you should type google20KB.png

Bonus:
The python file run.py can be used to run the rmiregistry and n peers.
Running as "python3 run.py" will create 10 peers (from 1000 to 1009).
Running as "python3 run.py -c" will compile before creating the 10 peers.
Running as "python3 run.py -n N" will create N peers (from 1000 to 1000+N).
