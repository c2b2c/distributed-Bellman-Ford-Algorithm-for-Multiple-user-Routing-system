

public class bfclient {

	public static void main(String[] args) {
		String IP = null;
		int localPort; 
		int timeOut;
		int degree;//number of links to the port
			
		localPort = Integer.parseInt(args[0]);
		timeOut = Integer.parseInt(args[1]);
		degree = (args.length-2)/3;
		LinkInfo [] linkSetup = new LinkInfo[degree];
		
		int Num=0;
		while(Num < degree){
			String setupIP = args[Num*3+2];
			int setupPort = Integer.parseInt(args[Num*3+3]);
			double setupCost = Double.parseDouble(args[Num*3+4]);
			
			LinkInfo link = new LinkInfo(setupIP,setupPort,setupCost);
			linkSetup[Num] = link;
			Num++;
		}
		Node newNode = new Node(IP,localPort,timeOut,linkSetup,degree);
		
	}

}
