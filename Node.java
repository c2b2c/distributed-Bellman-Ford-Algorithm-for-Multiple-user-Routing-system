
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.text.SimpleDateFormat;


public class Node {
	
	private final int bufSize = 1000;
	private int timeOut;
	
	private int degree;
    private NodeInfo localAddress;
    private DatagramSocket UDPSocket;
    private Timer timer1;
	private sendTimerTask taskTimer;
	private volatile LinkInfo linkInfo;
	private volatile ArrayList<LinkInfo> links;
	private volatile ArrayList<NeighborTimeoutTask> neighborTimer;
	//static int activeNode=0;
	

	
	public Node(String IP, int port, int timeOut, LinkInfo links[], int d){
		
		degree=d;		
		localAddress = new NodeInfo(null,port);		
		this.linkInfo = new LinkInfo(null,port,0);
		this.links = new ArrayList<LinkInfo>();
		this.timeOut = timeOut;		
		this.neighborTimer = new ArrayList<NeighborTimeoutTask>();
		timer1 = new Timer();
		
		int linkNum=0;
		//set up link and timer information for every link
		while(linkNum<links.length){
			this.links.add(links[linkNum]);
			
			NodeInfo neighborAddress = new NodeInfo(links[linkNum].localInfo().getIP(),links[linkNum].localInfo().getPort());
			this.linkInfo.addLink(neighborAddress, links[linkNum].getCost(), neighborAddress);

			NeighborSenderTask nsTask = new NeighborSenderTask(neighborAddress);
			timer1.schedule(nsTask, timeOut*1000);
			
			NeighborTimeoutTask ntTask = new NeighborTimeoutTask(neighborAddress);
			neighborTimer.add(ntTask);
			timer1.schedule(ntTask, 3*timeOut*1000);
			linkNum++;
		}
	   
		//set up UDPsocket for the port
		try {
			UDPSocket = new DatagramSocket(port);

		} catch (Exception e){}
		
		//receive and run the commands
		new InputOrder().start();
		OutputOrder();
	
	}
	
	//read in the orders
	private class InputOrder extends Thread{
		
		public InputOrder(){
		}
		
		public void run(){
			while(true){
				byte[] buffer = new byte[bufSize];  
				DatagramPacket Packet = new DatagramPacket(buffer, buffer.length);
				try {
					UDPSocket.receive(Packet);
				} catch (Exception e) {}
				
				byte[] src = Packet.getData();
				byte[] dest = new byte[Packet.getLength()];
				System.arraycopy(src, 0, dest, 0, Packet.getLength());
				
				String message = new String(dest);
				String [] msgSplit = message.split("\n");
				String order = msgSplit[0].split(":")[1];
				

				String sourceIP = Packet.getAddress().getHostAddress();
				int sourcePort = Packet.getPort();
				NodeInfo sourceNode = new NodeInfo(sourceIP,sourcePort);
				
				String destAddr = msgSplit[1];
				String [] destAddrSplit = destAddr.split(":");
								
				//When we receive update route table
				if(order.equals("ROUTEUPDATE")){
					
					if(localAddress.getIP()==null){
						localAddress.setIP(destAddrSplit[1]);
					}
					timerUpdate(sourceNode);
					int linkNum = Integer.parseInt(msgSplit[2].split(":")[1]);
					int numCounter=0;
					ArrayList<Link> updateLink = new ArrayList<Link>();
					while(numCounter<linkNum){
						String linkMsg = msgSplit[numCounter+3];
						String[] linkMsgSplit = linkMsg.split(" ");
						String IP = linkMsgSplit[0].split(":")[0];
						int port = Integer.parseInt(linkMsgSplit[0].split(":")[1]);
						NodeInfo linkAddr = new NodeInfo(IP,port);
						double resultCost = Double.parseDouble(linkMsgSplit[1]);
						
						//update neighbors' info
						Link linkTry = toRemote(linkAddr);
						if(linkTry!=null){
							if(linkTry.getLink()!=null){
								if(linkTry.getLink().equals(sourceNode)){
									double costTmp = toRemote(sourceNode).getCost();
									double costNew;
									if(costTmp<0 || resultCost<0) costNew = -1;
									else costNew = resultCost+costTmp;
									linkTry.setCost(costNew);
								}
							}
						}else{}
						
						Link tmpLink = new Link(linkAddr, resultCost,null);
						updateLink.add(tmpLink);

						numCounter++;
					}

					Link linkTmp = new Link(sourceNode, 0,null);
					updateLink.add(linkTmp);
					
					LinkInfo neighbor = findLink(sourceNode);
					double costTmp = -1;
					if(neighbor!=null){
						neighbor.setLinks(updateLink);
						if(neighbor.getCost()<0) neighbor.regetCost();
					}else{
						//It's a new neighbor.
						Iterator<Link> it = updateLink.iterator();
						Link tmpLink = null;
						
						while(it.hasNext()){
							tmpLink = it.next();
							if(tmpLink.getRemote().equals(localAddress)) costTmp = tmpLink.getCost();
						}
						LinkInfo newNeighbor = new LinkInfo(sourceIP,sourcePort,costTmp);
						newNeighbor.setLinks(updateLink);
						
						links.add(newNeighbor);						
						NeighborSenderTask nsTask = new NeighborSenderTask(sourceNode);
						timer1.schedule(nsTask, timeOut*1000);
						
						NeighborTimeoutTask ntTask = new NeighborTimeoutTask(sourceNode);
						neighborTimer.add(ntTask);
						timer1.schedule(ntTask, 3*timeOut*1000);

						if(toRemote(newNeighbor.localInfo())==null)
						linkInfo.addLink(sourceNode, costTmp, newNeighbor.localInfo());

					}
					

					Iterator<Link> it = updateLink.iterator();
					Link tmpLink = null;
					while(it.hasNext()){
						tmpLink = it.next();
						if(!tmpLink.getRemote().equals(localAddress)){
							if(toRemote(tmpLink.getRemote())==null){
								double newCost;
								if(tmpLink.getCost()<0 || costTmp<0) newCost=-1;
								else newCost = tmpLink.getCost()+costTmp;
								
								Link newLink = new Link(tmpLink.getRemote(),newCost,sourceNode);
								linkInfo.addLink(newLink);
							}
						}
					}
					costUpdate();
				}
				
				//Orders work
				
				if(order.equals("LINKDOWN")){
					LinkInfo neighbor = findLink(sourceNode);
					if(neighbor!=null){
						Iterator<Link> link = linkInfo.getLinks().iterator();
						while(link.hasNext()){
							Link linkTmp = link.next();
							if(linkTmp.getRemote().equals(localAddress)) continue;
							if(linkTmp.getLink().equals(sourceNode)) linkTmp.setCost(-1);
						}
						neighbor.setCost(-10);
					}
					costUpdate();
				}
				
				if(order.equals("LINKUP")){
					LinkInfo neighbor = findLink(sourceNode);
					if(neighbor!=null){
						neighbor.regetCost();
					}
					costUpdate();
				}
								
				
				if(order.equals("CHANGECOST")){
					String costLine = msgSplit[2];
					String [] costLineSplit = costLine.split(":");
					double newCost = Double.parseDouble(costLineSplit[1]);
					LinkInfo neighbor = findLink(sourceNode);
					if(neighbor!=null){
						neighbor.setCost(newCost);
						Iterator<Link> link = linkInfo.getLinks().iterator();
						while(link.hasNext()){
							Link linkTmp = link.next();
							if(linkTmp.getRemote().equals(localAddress)) continue;
							if(linkTmp.getLink().equals(sourceNode)) {
								linkTmp.setCost(-1);
							}
						}
					}
					costUpdate();
				}
			}
		}
	
	}

	// Output result accroding to commands
    int commandLines=0;
	
	private void OutputOrder(){
		while(true){
			commandLines++;
			System.out.print("\n Command number " + commandLines +" : ");
			String tmpStr = null;
			InputStreamReader reader = new InputStreamReader(System.in);
			BufferedReader in = new BufferedReader(reader);
            try {
				tmpStr = in.readLine();
			} catch (Exception e) {}
            
            if(tmpStr==null) continue;
            String [] strSplit = tmpStr.split(" ");
            //Case LINKDOWN, and handle wrong command
            if(strSplit[0].toUpperCase().equals("LINKDOWN")){
            	if(strSplit.length!=3){
            		logTime();
            		wrongCommand();
            		continue;
            	}
            	String IP = strSplit[1];
            	int port = 0;
            	try{
            		port = Integer.parseInt(strSplit[2]);
            	}catch(NumberFormatException e){
            		logTime();
            		wrongCommand();
            		continue;
            	}
            	NodeInfo downNode = new NodeInfo(IP,port);
            	this.linkDown(downNode);
            }
            //Case LINKUP, and handle wrong command
            else if(strSplit[0].toUpperCase().equals("LINKUP")){
            	if(strSplit.length!=3){
            		logTime();
            		wrongCommand();
            		continue;
            	}
            	String IP = strSplit[1];
            	int port = 0;
            	try{
            		port = Integer.parseInt(strSplit[2]);
            	}catch(NumberFormatException e){
            		logTime();
            		wrongCommand();
            		continue;
            	}
            	NodeInfo regetLink = new NodeInfo(IP,port);
            	this.linkUp(regetLink);
            }
            //CASE CHANGECOST, and handle wrong command
            else if(strSplit[0].toUpperCase().equals("CHANGECOST")){
            	if(strSplit.length!=4){
            		logTime();
            		wrongCommand();
            		continue;
            	}
            	String IP = strSplit[1];           	
            	int port = 0;
            	try{
            		port = Integer.parseInt(strSplit[2]);
            	}catch(NumberFormatException e){
            		logTime();
            		wrongCommand();
            		continue;
            	}
            	
            	double newCost = 0;
            	try{
            		newCost = Double.parseDouble(strSplit[3]);
            	}catch(NumberFormatException e){
            		logTime();
            		wrongCommand();
            		continue;
            	}
            	if(newCost<=0){
            		logTime();
            		System.out.println("The cost of link should be greater than 0.");
            		continue;
            	}
            	NodeInfo nbNode = new NodeInfo(IP,port);
            	changeCost(nbNode, newCost);
            }
            //Case SHOWRT
            else if(strSplit[0].toUpperCase().equals("SHOWRT")){
            	showrt();
            }
            //Case CLOSE
            else if(strSplit[0].toUpperCase().equals("CLOSE")){
            	close();
            }
            //Case SHOWDEGREE
            else if(strSplit[0].toUpperCase().equals("SHOWDEGREE")){
                showDegree();
            }
            else{
        		logTime();
        		wrongCommand();
        	}
		}
	}
	
	//Check if there's the link
	private LinkInfo findLink(NodeInfo Addr){
		Iterator<LinkInfo> it = this.links.iterator();
		LinkInfo link = null;
		while(it.hasNext()){
			LinkInfo neighbor = it.next();
			if(neighbor.localInfo().equals(Addr)){
				link = neighbor;
				break;
			}
		}
		return link;
	}
	
	//find remote node
	private Link toRemote(NodeInfo Addr){
		Link tmpLink = null;
		Iterator<Link> link1 = this.linkInfo.getLinks().iterator();
		while(link1.hasNext()){
			Link link = link1.next();
			if(link.getRemote().equals(Addr)){
				tmpLink = link;
				break;
			}
		}
		return tmpLink;
	}
	
	private String orderMsg(String order, NodeInfo desNode){
		String msg = "";
		msg += "Order:"+order+"\n" + "Destination:" + desNode.getIP() + ":" + desNode.getPort()+"\n";
		if(order.equals("ROUTEUPDATE")){
			msg += "Info:"+this.linkInfo.getLinks().size()+"\n";
			Iterator<Link> link1 = this.linkInfo.getLinks().iterator();
			while(link1.hasNext()){
				Link linkTmp = link1.next();
				String tmpMsg = "";
				tmpMsg += linkTmp.getRemote().getIP()+":"+linkTmp.getRemote().getPort();

				double costNew = linkTmp.getCost();

				if(linkTmp.getLink().equals(desNode) && !linkTmp.getRemote().equals(desNode)) costNew = -1;
				
				tmpMsg += " "+ costNew +"\n";
				msg += tmpMsg;
			}
		}
		return msg;
	}
	
	//link cost update
	private void costUpdate() {
		boolean flag = false;
		Iterator<Link> link1 = this.linkInfo.getLinks().iterator();
		while (link1.hasNext()) {
			Link originalLink = link1.next();
			if(originalLink.getRemote().equals(localAddress)) continue;

			double finalCost = -1;
			NodeInfo bestLink = originalLink.getLink();

			Iterator<LinkInfo> neighborIterator = this.links
					.iterator();
			while (neighborIterator.hasNext()) {
				LinkInfo neighbor = neighborIterator.next();
				Link newLink = neighbor.toRemote(originalLink
						.getRemote());
				if(newLink==null) continue;
				
				double newCost=0;
				if(newLink.getCost()>=0 && neighbor.getCost()>=0){
					newCost = newLink.getCost() + neighbor.getCost();
				}else{
					newCost = -1;
				}
				if ((finalCost > newCost && newCost>=0) || (finalCost<0 && newCost>=0)) {
					finalCost = newCost;
					bestLink = neighbor.localInfo();
				}
			}
			if ((originalLink.getCost() > finalCost && finalCost>=0) || (originalLink.getCost()<0 && finalCost>=0)){
				originalLink.setCost(finalCost);
				originalLink.setLink(bestLink);
				flag = true;
			}
			
			
		}
		if(flag) sendAll();
	}
	
	private void sendAll(){
		
		String msg="";
		byte [] Bmsg;
		
		Iterator<LinkInfo> nbIt = this.links.iterator();
		while(nbIt.hasNext()){
			LinkInfo neighbor = nbIt.next();
			if(neighbor.getCost()==-100) continue;
			msg = this.orderMsg("ROUTEUPDATE",neighbor.localInfo());
			Bmsg = msg.getBytes();
			String neighborIP = neighbor.localInfo().getIP();
			int neighborPort = neighbor.localInfo().getPort();
			InetAddress neighborInetAddress = null;
			try {
				neighborInetAddress = InetAddress.getByName(neighborIP);
			} catch (Exception e) {}
			DatagramPacket packet = new DatagramPacket(Bmsg,Bmsg.length,neighborInetAddress,neighborPort);
			try {
				UDPSocket.send(packet);
			} catch (Exception e) {}
			
		}
	}
	
	private void sendOne(NodeInfo Addr, String msg){
		byte [] Bmsg = msg.getBytes();
		InetAddress neighborInetAddress = null;
		try {
			neighborInetAddress = InetAddress.getByName(Addr.getIP());
		} catch (Exception e) {}
		DatagramPacket packet = new DatagramPacket(Bmsg,Bmsg.length,neighborInetAddress,Addr.getPort());
		try {
			UDPSocket.send(packet);
		} catch (Exception e) {}
	}
	
	//send all when timeout.
	private class sendTimerTask extends TimerTask{

		@Override
		public void run() {
			sendAll();
			taskTimer.cancel();
			taskTimer = new sendTimerTask();
			timer1.schedule(taskTimer, timeOut*1000);
		}	
	}
	
	
	//send one when timeout.
	private class NeighborSenderTask extends TimerTask{
		
		private NodeInfo neighborAddr;
		
		public NeighborSenderTask(NodeInfo neighborAddr){
			this.neighborAddr = neighborAddr;
		}

		@Override
		public void run() {
			LinkInfo neighbor = findLink(this.neighborAddr);
			if(neighbor==null) return;
			if(neighbor.getCost()>=0){
				String msgToSend = orderMsg("ROUTEUPDATE",neighbor.localInfo());
				sendOne(this.neighborAddr, msgToSend);
			}
			taskTimer = new sendTimerTask();
			timer1.schedule(taskTimer, timeOut*1000);
		}	
		
		public NodeInfo getneighborAddr(){
			return this.neighborAddr;
		}
	}
	
	private class NeighborTimeoutTask extends TimerTask{
		
		private NodeInfo neighborAddr;
		
		public NeighborTimeoutTask(NodeInfo neighborAddr){
			this.neighborAddr = neighborAddr;
		}
		
		public void run() {
			LinkInfo neighbor = findLink(this.neighborAddr);
			if(neighbor!=null){
				Iterator<Link> it = linkInfo.getLinks().iterator();
				while(it.hasNext()){
					Link linkTmp = it.next();
					if(linkTmp.getRemote().equals(localAddress)) continue;
					if(linkTmp.getLink().equals(this.neighborAddr)) {
						linkTmp.setCost(-1);
					}
				}
				neighbor.setCost(-100);
				costUpdate();
			}
		}	
		
		public NodeInfo getneighborAddr(){
			return this.neighborAddr;
		}
	}
	
	private void timerUpdate(NodeInfo Addr){

		Iterator<NeighborTimeoutTask> it = this.neighborTimer.iterator();
		while(it.hasNext()){
			NeighborTimeoutTask ntTask = it.next();
			if(ntTask.getneighborAddr().equals(Addr)){
				ntTask.cancel();
				neighborTimer.remove(ntTask);
				NeighborTimeoutTask newntTask = new NeighborTimeoutTask(Addr);
				timer1.schedule(newntTask, 3*1000*timeOut);
				this.neighborTimer.add(newntTask);
				break;
			}
		}

	}
	
	
	//run the commands
	
	//command linkDown
		private void linkDown(NodeInfo nodeAddr){
			LinkInfo neighbor = findLink(nodeAddr);
			if(neighbor!=null){
				Iterator<Link> link1 = linkInfo.getLinks().iterator();
				while(link1.hasNext()){
					Link link2 = link1.next();
					if(link2.getRemote().equals(localAddress)) continue;
					if(link2.getLink().equals(nodeAddr)) {
						link2.setCost(-1);
					}
				}
				sendOne(nodeAddr,orderMsg("LINKDOWN",nodeAddr));
				neighbor.setCost(-100);
				logTime();
				System.out.println("("+nodeAddr.getIP()+":"+nodeAddr.getPort()+") is linked down with localport.");
			}else{
				logTime();
				System.out.println("No neighbor("+nodeAddr.getIP()+":"+nodeAddr.getPort()+")!");
			}
			this.costUpdate();
		}
		//command linkUp
		private void linkUp(NodeInfo nodeAddr){
			//Check and set the cost.
			LinkInfo neighbor = findLink(nodeAddr);
			if(neighbor!=null){
				sendOne(nodeAddr,orderMsg("LINKUP",nodeAddr));
				neighbor.regetCost();
				logTime();
				System.out.println("("+nodeAddr.getIP()+":"+nodeAddr.getPort()+") is linked up with localport.");
			}else{
				logTime();
				System.out.println("No neighbor("+nodeAddr.getIP()+":"+nodeAddr.getPort()+")!");
			}
			this.costUpdate();
		}
		//command showrt
		private void showrt(){
			String msg = "";
			logTime();
			Iterator<Link> link1 = this.linkInfo.getLinks().iterator();
			while(link1.hasNext()){
				Link linkTmp = link1.next();
				if(linkTmp.getRemote().equals(this.localAddress)) continue;
				String linkInfoN = "";
				linkInfoN += "Destination = " + linkTmp.getRemote().getIP()+":"+linkTmp.getRemote().getPort()+", "+
				        "Cost = "+(linkTmp.getCost()<0?"Infinity":linkTmp.getCost())+", "+
						"Link = ("+linkTmp.getLink().getIP()+":"+linkTmp.getLink().getPort()+")";
				if(link1.hasNext()) linkInfoN+= "\n";
				msg += linkInfoN;
			}
			System.out.println(msg);		
			
		}
		//command changeCost
		private void changeCost(NodeInfo nodeAddr, double newCost){
			LinkInfo neighbor = findLink(nodeAddr);
			if(neighbor!=null){
				String msg = "";
				msg += "Order:CHANGECOST\n"
				    + "Destination:" + nodeAddr.getIP() + ":" + nodeAddr.getPort()+"\n"
	                + "NewCost:" + newCost;
				sendOne(nodeAddr,msg);
				Iterator<Link> link = linkInfo.getLinks().iterator();
				while(link.hasNext()){
					Link link1 = link.next();
					if(link1.getRemote().equals(localAddress)) continue;
					if(link1.getLink().equals(nodeAddr)) {
						link1.setCost(-1);
					}
				}
				neighbor.setCost(newCost);
				logTime();
				System.out.println("The cost of link ("+nodeAddr.getIP()+":"+nodeAddr.getPort()+") is changed.");
			}else{
				logTime();
				System.out.println("No neighbor ("+nodeAddr.getIP()+":"+nodeAddr.getPort()+")!");
			}
			this.costUpdate();
		}
		//command showdegree
		private void showDegree(){
			logTime();
			System.out.println("The degree of the local node is " + degree + ".");
			
		}
		//command close
		private void close(){
			logTime();
			System.out.println("This node is closed in the network! \n"); 
			System.exit(1);
		}

		//Function orders
		
		//log the time of every output	
		private void logTime(){
			Calendar log = Calendar.getInstance();
			SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			System.out.println("<LOG : " + formatter.format(log.getTime())+"> ");
		}	
		//show right format if command is wrong
		private void wrongCommand(){
			System.out.println("The command is wrong. "); 
			System.out.println("Right commands: \n"
					+ "LINKDOWN IP port \n"
					+ "LINKUP IP port \n"
					+ "CHANGECOST IP port cost \n"
					+ "SHOWRT \n"
					+ "SHOWDEGREE \n"
					+ "CLOSE");
		}
}
