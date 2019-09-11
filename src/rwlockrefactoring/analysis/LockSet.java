package rwlockrefactoring.analysis;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.ipa.callgraph.propagation.HeapModel;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.cha.ClassHierarchy;

public class LockSet {
	
	PointerAnalysis<InstanceKey> pointer;
	ClassHierarchy cha;
	Map<IField,String> lockmap=new HashMap<IField, String>();
	Map<IField,String> slockmap=new HashMap<IField, String>();
	Map<String,String> thismap=new HashMap<String,String>();
	Map<String,String> staticmap=new HashMap<String,String>();
	
	public LockSet(PointerAnalysis<InstanceKey> p,ClassHierarchy c){
		pointer=p;
		cha=c;
	}
	
	public void lockmap(List<String> lockex,String type) {
		Iterator<IClass> ic=cha.iterator();
		//System.out.println(lockex);
		while(ic.hasNext()) {
			IClass iclass=ic.next();
			if(iclass.getName().getClassName().toString().equals(type)) {
				for(int i=0;i<lockex.size();i++) {
					if(lockex.get(i).toString().equals("this")) {
						thismap.put("this", "tlock");
					}else if(lockex.get(i).toString().equals("static")&&lockex.get(i).toString().equals("getClass")){
						staticmap.put("static", "stlock");
					}else {
				Iterator<IField> ifield=iclass.getAllFields().iterator();
				while(ifield.hasNext()) {
					IField field=ifield.next();
					if(field.getName().toString().equals(lockex.get(i))) {
						//°ÑÊµÀý×Ö¶ÎºÍ¾²Ì¬×Ö¶Î·Ö±ð´æ´¢
						if(field.isStatic()) {
							slockmap.put(field, null);
						}else {
							lockmap.put(field, null);
						}
					}
				}
			}
					}
			}
			
		}
	} 
	
	public void getpointer() {

		Collection<InstanceKey> co=pointer.getInstanceKeys();
		Iterator<InstanceKey> io=co.iterator();
		Iterator<IClass> ic=cha.iterator();
		HeapModel hm=pointer.getHeapModel();
		InstanceKey o=null;
		while(io.hasNext()) {
			InstanceKey oo=(InstanceKey)io.next();
			if(oo.getConcreteType().getName().toString().equals("Ltest/ss/wala/A")) {
				o=oo;
			}
		}
		PointerKey pp=null;
		PointerKey ppp=null;
		while(ic.hasNext()) {
			IClass iclass=ic.next();
			if(iclass.getName().getClassName().toString().equals("A")) {
				Iterator<IField> ifield=iclass.getAllFields().iterator();
				while(ifield.hasNext()) {
					IField field=ifield.next();
					if(field.getName().toString().equals("o")) {
						IField resolveAgain =
						        o.getConcreteType().getField(field.getName(), field.getFieldTypeReference().getName());
						pp=hm.getPointerKeyForInstanceField(o,field);
					}else if(field.getName().toString().equals("ab")) {
						ppp=hm.getPointerKeyForStaticField(field);
						
					}
				}
			}
			
		}
	}
	
	public void stLock() {
		if(slockmap.size()!=0) {
		Set<IField> keyset=slockmap.keySet();
		AliasAnalysis alias=new AliasAnalysis(pointer);
		IField[] fields=new IField[keyset.size()];
		HeapModel hm=pointer.getHeapModel();
		PointerKey pp=null;
		PointerKey ppp=null;
		int k=0;
		for(IField f:keyset) {
			fields[k]=f;
			k++;
		}
		slockmap.put(fields[0],"lock");
		for(int i=0;i<k;i++) {
			pp=hm.getPointerKeyForStaticField(fields[i]);
			for(int j=i+1;j<k;j++) {
				ppp=hm.getPointerKeyForStaticField(fields[j]);
				if(alias.mayAlias(pp, ppp)) {
					slockmap.put(fields[j], slockmap.get(fields[i]));
				}else {
					slockmap.put(fields[j],"lock"+j);
				}
			}
		}
		}
	}
	
	public void inLock() {
		if(lockmap.size()!=0) {
		Set<IField> keyset=lockmap.keySet();
		Map<IField,InstanceKey> tmp=new HashMap<IField, InstanceKey>();
		Collection<InstanceKey> co=pointer.getInstanceKeys();
		Iterator<InstanceKey> io=co.iterator();
		AliasAnalysis alias=new AliasAnalysis(pointer);
		IField[] fields=new IField[keyset.size()];
		HeapModel hm=pointer.getHeapModel();
		PointerKey pp=null;
		PointerKey ppp=null;
		int k=0;
		for(IField f:keyset) {
			fields[k]=f;
			k++;
		}
		lockmap.put(fields[0],"ilock");
		for(IField f:keyset) {
		InstanceKey o=null;
		while(io.hasNext()) {
			InstanceKey oo=(InstanceKey)io.next();
			if(oo.getConcreteType().getReference().toString().equals(f.getFieldTypeReference().toString())) {
				o=oo;
				System.out.println("youma"+f);
				tmp.put(f,o);
				}
			}
		}
		System.out.println("tmp-->"+tmp);
		for(int i=0;i<k;i++) {
			if(tmp.get(fields[i])!=null) {
			pp=hm.getPointerKeyForInstanceField(tmp.get(fields[i]),fields[i]);
			for(int j=i+1;j<k;j++) {
				ppp=hm.getPointerKeyForInstanceField(tmp.get(fields[j]),fields[j]);
				if(alias.mayAlias(pp, ppp)) {
					lockmap.put(fields[j], lockmap.get(fields[i]));
				}else {
					lockmap.put(fields[j],"ilock"+j);
				}
			}}else {
				lockmap.put(fields[i],"nlock"+i);
			}
		}
		}
		System.out.println(lockmap);
	}
	
	public Map<IField,String> getsmap(){
		return slockmap;
	}
	public Map<IField,String> getmap(){
		return lockmap;
	}
	
	public Map<String,String> thismap(){
		return thismap;
	}
	
	public Map<String,String> staticmap(){
		return staticmap;
	}

}
