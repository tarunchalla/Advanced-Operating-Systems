Advanced-Operating-Systems
==========================

Advanced Operating Systems

Implement Maekawa’s distributed mutual exclusion algorithm with deadlock handling [1].There are N nodes in the
system, numbered from 0 to N 􀀀 1. The requirements for each node are:
1. Each node communicates through persistent TCP connections to other nodes.
2. It reads an input file (“input.txt”) and populates its quorum. The file also contains inter-request time (IR) and
critical section time (CST), both in milliseconds. IR is the time between an exit from the critical section and
the next request for critical section entry. CST represents the time interval that each process will spend in the
critical section, once it gets the access.
3. Each process generates M number of CS requests (given in “input.txt”).
4. Once it gets access to CS, it sends a message to a designated node, CSNode. The message contains <NODE-ID,REQ-ID,CST>.
The CS execution finishes when the process receives ACK from CSNode.
5. Node zero has additional responsibility to send an initial message to all other nodes to start the execution. It also
collects and display the collected data from all nodes (including itself) and indicates termination of the program.
4 Critical Section
Implement a separate program, CSNode, that runs on a separate machine. CSNode accepts a TCP connection on a
listening socket, spawn a thread to execute the CS request. It opens a file “logs.txt”. For execution, it does following
in the given sequence:
1. Write <SENDER-ID,REQ-ID,START-TIME> to the file.
2. Sleeps CST amount of time.
3. Write <SENDER-ID,REQ-ID,FINISH-TIME> to the file.
4. Sends ACK to the sender notifying that CS request is done.
1
5 Data Collection
For your implementation of the mutual exclusion algorithm, each node must collect the following:
For each of its CS request:
1. Number of REQUESTs sent.
2. Number of REPLYs received.
3. Number of RELEASEs sent.
4. Number of FAILs received.
5. Number of ENQUIREs received.
6. Number of YEILDs sent.
Once a process finished M CS requests it send the collected data to node zero.
6 Submission Information
The submission should be through elearning in the form of an archive (.zip, .tar or .gz) consisting of:
1. File(s) containing only the source code.
2. The README file, which describes how to run your program.
NOTE: Do not submit unnecessary files.
References
[1] Maekawa Mamoru, A
p
N algorithm for mutual exclusion in decentralized systems, ACM Trans. Comput. Syst.,
pg.145–159, vol.-3, No.2, 1985.
