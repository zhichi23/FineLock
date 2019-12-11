package rwlockrefactoring.analysis;

import java.util.List;
/**
 * 重构的前置条件
 * @author shao
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
