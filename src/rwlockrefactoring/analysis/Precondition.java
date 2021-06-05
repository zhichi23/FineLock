package rwlockrefactoring.analysis;

import java.util.List;
/**
 * 
 * @author Shuai
 *
 */
public class Precondition {
	
	List<String> readlist;
	List<String> writelist;
	
	public Precondition(List<String> readlist,List<String> writelist) {
		this.readlist=readlist;
		this.writelist=writelist;
	}
	
	public boolean canRefactor() {
		for(String f:readlist) {
			if(writelist.contains(f)) {
				return false;
			}
		}
		return true;
	}

}
