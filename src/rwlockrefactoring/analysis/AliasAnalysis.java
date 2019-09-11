package rwlockrefactoring.analysis;

import java.util.List;

import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.util.intset.OrdinalSet;

public class AliasAnalysis {
	
	PointerAnalysis<InstanceKey> pa;
	
	public AliasAnalysis(PointerAnalysis<InstanceKey> pa){
		this.pa = pa;
	}

	/**
	 * may-alias analysis for two PointerKeys
	 */
	public boolean mayAlias(PointerKey pk1, PointerKey pk2) {
		if(pk1 == null || pk2 == null)
			return false;
		OrdinalSet<InstanceKey> ptsTo1 = pa.getPointsToSet(pk1);
		OrdinalSet<InstanceKey> ptsTo2 = pa.getPointsToSet(pk2);
		for (InstanceKey i : ptsTo1) {
			for (InstanceKey j : ptsTo2) {
				if (i.equals(j)) {
					return true;
				}
			}
		}
		return false;
	}
	
	/**
	 * may-alias analysis 
	 */
	public boolean mayAlias(List<PointerKey> pointerKeyList, PointerKey pointerKey){
		if(pointerKeyList == null || pointerKeyList.isEmpty() || pointerKey == null)
			return false;
		for(PointerKey pk : pointerKeyList){
			if(mayAlias(pk, pointerKey)){
//				System.out.println(pk);
//				System.out.println(pk2);
				return true;
			}
		}
		return false;
	}
	
	public boolean mayAlias(PointerKey pointerKey, InstanceKey instanceKey){
		if(pointerKey == null || instanceKey == null)
			return false;
		OrdinalSet<InstanceKey> pointTo = pa.getPointsToSet(pointerKey);
		for(InstanceKey ik : pointTo){
			if(ik.equals(instanceKey))
				return true;
		}
		return false;
	}
	
	public boolean mayAlias(List<PointerKey> pointerKeyList, InstanceKey instanceKey){
		if(pointerKeyList == null || pointerKeyList.isEmpty() || instanceKey == null)
			return false;
		for(PointerKey pk : pointerKeyList){
			if(mayAlias(pk, instanceKey))
				return true;
		}
		return false;
	}

}