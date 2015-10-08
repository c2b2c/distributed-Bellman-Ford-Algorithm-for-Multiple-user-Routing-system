
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Iterator;


public class LinkInfo {

	private double cost;
	private double setupCost;
	private NodeInfo local;
	private ArrayList<Link> links;
	
	public LinkInfo(String IP, int port, double c) {
		this.local = new NodeInfo(IP, port);
		this.cost = c;
		this.setupCost = c;
		this.links = new ArrayList<Link>();
	}
	
	
	public NodeInfo localInfo() {
		return this.local;
	}	
	//cost information
	public double getCost(){
		return this.cost;
	}
	public void setCost(double c){
		this.cost = c;
	}
	public void regetCost(){
		this.cost = this.setupCost;
	}
	//link information
	public ArrayList<Link> getLinks(){
		return this.links;
	}	
	public void setLinks(ArrayList<Link> l){
		this.links = l;
	}
	public void addLink(NodeInfo remote, double c, NodeInfo l){
		Link link = new Link(remote, c, l);
		this.links.add(link);
	}	
	public void addLink(Link link){
		this.links.add(link);
	}	
	public Link toRemote(NodeInfo add){
		Iterator<Link> links = this.links.iterator();
		Link link = null;
		while(links.hasNext()){
			Link link1 = links.next();
			if(link1.getRemote().equals(add)){
				link = link1;
				break;
			}
		}
		return link;
	}	
	

}