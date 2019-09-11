package rwlockrefactoring.nfa;


public class DirectedDFS {
	private boolean[] marked;
	public DirectedDFS(Digraph G,int s){
        marked = new boolean[G.V()];
        dfs(G,s);
    }
	public DirectedDFS(Digraph G,Iterable<Integer> sources){
        marked = new boolean[G.V()];//source相当于一个迭代器类型的数组
        for(int s:sources)
            if(!marked[s])
                dfs(G,s);
    }
	private void dfs(Digraph G,int v){
        marked[v] = true;
        for(int w:G.adj(v))//每一层递归遍历当前节点的邻接表，如果邻接表里的结点没被访问过，就访问下去
            if(!marked[w]) 
                dfs(G,w);
    }
    public boolean marked(int v){
        return marked[v];
    }
}