public class Link {
	
	private NodeInfo remote;
	private double cost;
	private NodeInfo setupLink;

	public Link(NodeInfo remote, double cost, NodeInfo setupLink) {
		this.remote = remote;
		this.cost = cost;
		this.setupLink = setupLink;
	}
	
	public NodeInfo getRemote() {
		return this.remote;
	}	
	// link information
	public NodeInfo getLink(){
		return this.setupLink;
	}	
	public void setLink(NodeInfo l){
		this.setupLink = l;
	}
	// cost information
	public double getCost() {
		return this.cost;
	}
	public void setCost(double c) {
		this.cost = c;
	}
	

	

}