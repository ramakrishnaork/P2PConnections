To run the program first initialize the first node of the network using the following command 
port 'portno' setdirectory 'folderpath'

Now to connect to this node use the following 
port 'portno' setdirectory 'folderpath' connect 'ipaddress:port of node to connect'

To run a query run the query command for a new node 
port 'portno' setdirectory 'folderpath' connect 'ipaddress:port of node to connect' query 'filename timetolive'

Example commands:

for initial node
port 1000 setdirectory C:\Users\smada\Desktop\PA4Files\P1\ 

to connect to first node
port 2000 setdirectory C:\Users\smada\Desktop\PA4Files\P1\ connect localhost:1000 
 
for query for filename abc.txt 
port 1000 setdirectory C:\Users\smada\Desktop\PA4Files\P1\ connect localhost:2000 query abc.txt 2000 