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

/**
 * 
 * @author Shuai
 * @version 2.0
 * 
 */
public class LockSet {

	PointerAnalysis<InstanceKey> pointer;
	ClassHierarchy cha;

	// for synchronized method
	// instance field
	Map<IField, String> ins_field_map = new HashMap<IField, String>();
	// static field
	Map<IField, String> st_field_lockmap = new HashMap<IField, String>();

	// for synchronized block
	// instance field
	Map<String, String> this_map = new HashMap<String, String>();
	// static field
	Map<String, String> static_map = new HashMap<String, String>();

	String type = null;

	public LockSet(PointerAnalysis<InstanceKey> p, ClassHierarchy c) {
		pointer = p;
		cha = c;
	}

	/**
	 * mapping static field and instance field respectively
	 * 
	 * @param lockex lock expression
	 * @param type   class
	 */
	public void lockmap(List<String> lockex, String type) {
		Iterator<IClass> ic = cha.iterator();
		this.type = type;
		while (ic.hasNext()) {
			IClass iclass = ic.next();
			if (iclass.getName().getClassName().toString().equals(type)) {
				for (int i = 0; i < lockex.size(); i++) {
					if (lockex.get(i).toString().equals("this")) {
						this_map.put("this", "tlock");
					} else if (lockex.get(i).toString().equals("static")
							|| lockex.get(i).toString().equals("getClass")) {
						static_map.put("static", "stlock");
					} else {
						Iterator<IField> ifield = iclass.getAllFields().iterator();
						while (ifield.hasNext()) {
							IField field = ifield.next();
							if (field.getName().toString().equals(lockex.get(i))) {
								if (field.isStatic()) {
									st_field_lockmap.put(field, null);
								} else {
									ins_field_map.put(field, null);
								}
							}
						}
					}
				}
			}

		}
	}

	@Deprecated
	public void getpointer() {
		Collection<InstanceKey> co = pointer.getInstanceKeys();
		Iterator<InstanceKey> io = co.iterator();
		Iterator<IClass> ic = cha.iterator();
		HeapModel hm = pointer.getHeapModel();
		InstanceKey o = null;
		while (io.hasNext()) {
			InstanceKey oo = (InstanceKey) io.next();
			if (oo.getConcreteType().getName().toString().equals("Ltest/ss/wala/A")) {
				o = oo;
			}
		}
		PointerKey pp = null;
		PointerKey ppp = null;
		while (ic.hasNext()) {
			IClass iclass = ic.next();
			if (iclass.getName().getClassName().toString().equals("A")) {
				Iterator<IField> ifield = iclass.getAllFields().iterator();
				while (ifield.hasNext()) {
					IField field = ifield.next();
					if (field.getName().toString().equals("o")) {
						IField resolveAgain = o.getConcreteType().getField(field.getName(),
								field.getFieldTypeReference().getName());
						pp = hm.getPointerKeyForInstanceField(o, field);
					} else if (field.getName().toString().equals("ab")) {
						ppp = hm.getPointerKeyForStaticField(field);

					}
				}
			}

		}
	}

	public void makelock() {
		inLock();
		stLock();
	}

	public void stLock() {
		// static field
		if (st_field_lockmap.size() != 0) {
			Set<IField> keyset = st_field_lockmap.keySet();
			AliasAnalysis alias = new AliasAnalysis(pointer);
			IField[] fields = new IField[keyset.size()];
			HeapModel hm = pointer.getHeapModel();
			PointerKey pp = null;
			PointerKey ppp = null;
			int k = 0;
			for (IField f : keyset) {
				fields[k] = f;
				k++;
			}
			st_field_lockmap.put(fields[0], "lock");
			// alias analysis
			for (int i = 0; i < k; i++) {
				pp = hm.getPointerKeyForStaticField(fields[i]);
				for (int j = i + 1; j < k; j++) {
					ppp = hm.getPointerKeyForStaticField(fields[j]);
					if (alias.mayAlias(pp, ppp)) {
						st_field_lockmap.put(fields[j], st_field_lockmap.get(fields[i]));
					} else {
						st_field_lockmap.put(fields[j], "lock" + j);
					}
				}
			}
		}
	}

	public void inLock() {
		if (ins_field_map.size() != 0) {
			Set<IField> keyset = ins_field_map.keySet();
			Map<IField, InstanceKey> tmp = new HashMap<IField, InstanceKey>();
			Collection<InstanceKey> co = pointer.getInstanceKeys();

			AliasAnalysis alias = new AliasAnalysis(pointer);
			IField[] fields = new IField[keyset.size()];
			HeapModel hm = pointer.getHeapModel();
			PointerKey pp = null;
			PointerKey ppp = null;
			int k = 0;
			for (IField f : keyset) {
				fields[k] = f;
				k++;
			}
			ins_field_map.put(fields[0], "ilock");
			for (IField f : keyset) {
				InstanceKey o = null;
				Iterator<InstanceKey> io = co.iterator();
				while (io.hasNext()) {
					InstanceKey oo = (InstanceKey) io.next();
					if (oo.getConcreteType().getReference().toString()
							.equals(f.getReference().getDeclaringClass().toString())) {
						o = oo;
						tmp.put(f, o);
					}
				}
			}

			for (int i = 0; i < k; i++) {
				if (tmp.get(fields[i]) != null) {
					pp = hm.getPointerKeyForInstanceField(tmp.get(fields[i]), fields[i]);
					for (int j = i + 1; j < k; j++) {
						ppp = hm.getPointerKeyForInstanceField(tmp.get(fields[j]), fields[j]);
						if (alias.mayAlias(pp, ppp)) {
							ins_field_map.put(fields[j], ins_field_map.get(fields[i]));
						} else {
							ins_field_map.put(fields[j], "ilock" + j);
						}
					}
				} else {
					ins_field_map.put(fields[i], "nlock" + i);
				}
			}
		}
	}

	public Map<IField, String> getsmap() {
		return st_field_lockmap;
	}

	public Map<IField, String> getmap() {
		return ins_field_map;
	}

	public Map<String, String> thismap() {
		return this_map;
	}

	public Map<String, String> staticmap() {
		return static_map;
	}

}
