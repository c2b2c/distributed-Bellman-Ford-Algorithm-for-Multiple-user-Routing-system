
public class NodeInfo {
	private String IP;
	private int port;

	public NodeInfo(String IP, int port) {
		this.IP = IP;
		this.port = port;
	}
	//IP information
	public String getIP() {
		return this.IP;
	}	
	public void setIP(String IP){
		this.IP = IP;
	}
	//port information
	public int getPort() {
		return this.port;
	}
	public void setPort(int port) {
		this.port=port ;
	}
	
	public boolean equals(Object OB){ 
		if(!(OB instanceof NodeInfo)) 
			return false; 
		NodeInfo addr = (NodeInfo) OB; 
		if (this.IP==null || addr.IP==null) return true; 
		return addr.IP.equals(IP) && addr.port == port; 
	}

}