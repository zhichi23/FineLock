package rwlockrefactoring.nfa;

import java.util.ArrayList;

public class Digraph {
	private final int V;//顶点个数
	private int E;//边的个数
	private ArrayList<Integer>[] adj;//邻接表
	public Digraph(int V){
		this.V=V;
		this.E=0;
		adj =  (ArrayList<Integer>[]) new ArrayList[V];
		for(int v=0;v<V;v++)
            adj[v] = new ArrayList<Integer>();
	}
	public void addEdge(int v,int w){
        adj[v].add(w);
        E++;
    }
    public int V(){
        return V;
    }
    public Iterable<Integer> adj(int v){
        return (Iterable<Integer>)adj[v]; 
    }
    
}
