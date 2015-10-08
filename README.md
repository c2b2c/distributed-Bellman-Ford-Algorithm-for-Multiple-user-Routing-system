# distributed-Bellman-Ford-Algorithm-for-Multiple-user-Routing-system
distributed Bellman-Ford Algorithm for Multiple-user Routing system

a. Details on development environment
I use eclipse and java 1.6 to develop my code.(JavaSE-1.6)
No other coding environment is needed.

b. Instructions on how to run your code
The code can be run on multiple command windows on a machine.
1.Go to the folder of the scripts, a single compile command is needed
“javac bfclient.java”
Then all files are ready to run
2.To invoke the program, a single line of order is needed: 
“java bfclient localport timeout [ipaddress1 port1 weight1 …]”
for example, according to the graph in the instructions, for port 4115:
java bfclient 4115 3 127.0.0.1 4116 5.0 127.0.0.1 4118 30.0

After nodes are set up, the route table can be seen after the user input the command.


c. Sample commands to invoke your code

There are 6 functions in clients,they can be involked as:
1.LINKDOWN IP port             :link down to the node of IP port
2.LINKUP IP port               :link up to the node of IP port
3.CHANGECOST IP port cost      :change the cost of link to the node of IP port
4.SHOWRT                       :show the route table
5.SHOWDEGREE                   :show the degree(links that set up at the beginning) of the local node
6.CLOSE                        :close the client

others that if you input wrong password or the user is already online will have instructions to follow.


d. Description of any additional functionalities and how they should be
executed/tested.
1. For every client, I show the how many command he has done on this client machine. For example:
 Command Line 3 : LINKDOWN 127.0.0.1 4115

2. For every function that is done, there will be a log showing the time of it. For example:
<LOG : 2014-12-13 13:26:05> 
The cost of link (127.0.0.1:4115) is changed.

3. I add a function called SHOWDEGREE, which can show the degree of the local node(How many links it has with others). For example:
 Command Line 5 : SHOWDEGREE    
<LOG : 2014-12-13 13:42:16> 
(The degree of the local node is 3.

4.I add a function called CHANGECOST, which can change the cost of adjacent links of the local node.For example:
 Command Line 8 : CHANGECOST 127.0.0.1 4115 100
<LOG : 2014-12-13 13:44:31> 
The cost of link (127.0.0.1:4115) is changed.

5. For every wrong command, I will show all right commands to the user. For example:
 Command Line 9 : CHANGECOST 127.0.0.14115 100
<LOG : 2014-12-13 13:44:07> 
The command is wrong. 
Right commands: 
LINKDOWN IP port 
LINKUP IP port 
CHANGECOST IP port cost 
SHOWRT 
SHOWDEGREE 
CLOSE

e.Textual description of design protocol including syntax and semantics
The protocol will receive UDP packets, and the message in that is (orderformat+order++Destination IP+ Destination Port + Others)
order convey the command, for example ROUTEUPDATE is to update the route table of the node,CHANGECOST is to change the cost form the node to other points. Also, for ROUTEDATE “Others” is useless, yet for CHANGECOST, “Others” will be the new cost of the link.
With correct decoding of the transmitted message, nodes can hear from each other, and update information according to the timeout time.
