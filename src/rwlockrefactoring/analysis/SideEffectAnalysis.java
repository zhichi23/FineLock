package rwlockrefactoring.analysis;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.classLoader.IBytecodeMethod;
import com.ibm.wala.ipa.callgraph.AnalysisCacheImpl;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.CallGraphBuilderCancelException;
import com.ibm.wala.ipa.callgraph.IAnalysisCacheView;
import com.ibm.wala.ipa.callgraph.impl.Everywhere;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.summaries.SummarizedMethod;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAAbstractBinaryInstruction;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.ssa.SSABinaryOpInstruction;
import com.ibm.wala.ssa.SSACFG;
import com.ibm.wala.ssa.SSACheckCastInstruction;
import com.ibm.wala.ssa.SSAConditionalBranchInstruction;
import com.ibm.wala.ssa.SSAConversionInstruction;
import com.ibm.wala.ssa.SSAFieldAccessInstruction;
import com.ibm.wala.ssa.SSAGetCaughtExceptionInstruction;
import com.ibm.wala.ssa.SSAGetInstruction;
import com.ibm.wala.ssa.SSAGotoInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.ssa.SSAMonitorInstruction;
import com.ibm.wala.ssa.SSANewInstruction;
import com.ibm.wala.ssa.SSAOptions;
import com.ibm.wala.ssa.SSAPhiInstruction;
import com.ibm.wala.ssa.SSAPiInstruction;
import com.ibm.wala.ssa.SSAPutInstruction;
import com.ibm.wala.ssa.SSAReturnInstruction;
import com.ibm.wala.types.MethodReference;

import rwlockrefactoring.util.RWSign;
import rwlockrefactoring.util.RWString;

/**
 * 
 * 
 * 
 * @author Shao
 * @version 4.0_01
 */
public class SideEffectAnalysis {

	boolean frist = true;
	int l = 0;
	int deep = 0;
	int invo=0;

	CallGraph cg;
	Map<Integer, Integer> linenum = new HashMap<Integer, Integer>();
	List<Integer> writlist = new ArrayList<Integer>();
	Map<Integer, List<String>> map = new HashMap<Integer, List<String>>();
	List<String> list;
	List<SSAInstruction> instructions = new ArrayList<SSAInstruction>();

	List<Integer> wl = new ArrayList<Integer>();
	List<Integer> rl = new ArrayList<Integer>();

	public SideEffectAnalysis(CallGraph cg) {
		this.cg = cg;

	}

	public String sideEffect(String method) throws IllegalArgumentException, CallGraphBuilderCancelException,
			ClassHierarchyException, IOException, UnsupportedOperationException, InvalidClassFileException {
		// 构建调用图
		// CallGraph cg = callgraph();
		StringBuffer sb = new StringBuffer();
		IAnalysisCacheView cache = new AnalysisCacheImpl();
		int if_index = 0;
		int else_index = Integer.MAX_VALUE;
		int line = 0;
		int bytecodeIndex, sourceLineNum;
		for (CGNode n : cg) {
			if (n.getMethod().getName().toString().equals(method)) {
				
				IR ir = cache.getIR(n.getMethod(), Everywhere.EVERYWHERE);
				List<SSAInstruction> instructions = new ArrayList<SSAInstruction>();
				if (ir == null || ir.getMethod() instanceof SummarizedMethod) {
					return null;
				}
				IBytecodeMethod bytemethod = (IBytecodeMethod) ir.getMethod();
				// 串
				// Iterator<CGNode> iter=cg.getSuccNodes(n);
				List<String> tmp = new LinkedList<String>();
				Iterator<SSAInstruction> instruction = ir.iterateAllInstructions();

				// 遍历方法中的所有指令
				while (instruction.hasNext()) {
					SSAInstruction ins = instruction.next();
					instructions.add(ins);
				}

				int tm = instructions.get(instructions.size() - 1).iindex;
				int k = 1;
				while (tm == -1) {
					tm = instructions.get(instructions.size() - k).iindex;
					k++;
				}

				for (int in = 0; in <= tm; in++) {
					bytecodeIndex = bytemethod.getBytecodeIndex(in);
					sourceLineNum = bytemethod.getLineNumber(bytecodeIndex);
					// int sourceposition=method.getSourcePosition(ins.iindex);
					if (!linenum.containsKey(sourceLineNum)) {
						linenum.put(sourceLineNum, line);
						line++;
					}
				}
				Iterator<Integer> set = linenum.keySet().iterator();
				while (set.hasNext()) {
					list = new ArrayList<String>();
					map.put(linenum.get(set.next()), list);
				}
				while (l < instructions.size()) {
					sb.append(geteffect(instructions.get(l), n, bytemethod));
				}
				return sb.toString();
			}

		}
		return null;

	}

	public String sideEffect(String method, String cla)
			throws IllegalArgumentException, CallGraphBuilderCancelException, ClassHierarchyException, IOException,
			UnsupportedOperationException, InvalidClassFileException {
		// 构建调用图
		// CallGraph cg = callgraph();
		StringBuffer sb = new StringBuffer();
		IAnalysisCacheView cache = new AnalysisCacheImpl();
		
		int line = 0;
		int bytecodeIndex, sourceLineNum;
		for (CGNode n : cg) {
			if (n.getMethod().getName().toString().equals(method)
					&& n.getMethod().getDeclaringClass().getName().getClassName().toString().equals(cla)) {
				IR ir = cache.getIR(n.getMethod(), Everywhere.EVERYWHERE);
				
				if (ir == null|| ir.getMethod() instanceof SummarizedMethod) {
					return null;
				}
				IBytecodeMethod bytemethod = (IBytecodeMethod) ir.getMethod();
				// 串
				List<String> tmp = new LinkedList<String>();
				Iterator<SSAInstruction> instruction = ir.iterateAllInstructions();

				// 遍历方法中的所有指令
				while (instruction.hasNext()) {
					SSAInstruction ins = instruction.next();
					instructions.add(ins);
				}

				int tm = instructions.get(instructions.size() - 1).iindex;
				int k = 1;
				while (tm == -1) {
					tm = instructions.get(instructions.size() - k).iindex;
					k++;
				}
				for (int in = 0; in <= tm; in++) {
					bytecodeIndex = bytemethod.getBytecodeIndex(in);
					sourceLineNum = bytemethod.getLineNumber(bytecodeIndex);
					// int sourceposition=method.getSourcePosition(ins.iindex);
					if (!linenum.containsKey(sourceLineNum)) {
						linenum.put(sourceLineNum, line);
						line++;
					}
				}
				Iterator<Integer> set = linenum.keySet().iterator();
				while (set.hasNext()) {
					list = new ArrayList<String>();
					map.put(linenum.get(set.next()), list);
				}
				while (l < instructions.size()) {
					sb.append(geteffect(instructions.get(l), n, bytemethod));
				}
				return sb.toString();
			}

		}
		return null;

	}
	
	@SuppressWarnings("rawtypes")
	public String sideEffect(String method, String cla,List p)
			throws IllegalArgumentException, CallGraphBuilderCancelException, ClassHierarchyException, IOException,
			UnsupportedOperationException, InvalidClassFileException {
		// 构建调用图
		// CallGraph cg = callgraph();
		StringBuffer sb = new StringBuffer();
		IAnalysisCacheView cache = new AnalysisCacheImpl();
		int if_index = 0;
		int else_index = Integer.MAX_VALUE;
		int line = 0;
		int bytecodeIndex, sourceLineNum;
		for (CGNode n : cg) {
			if (n.getMethod().getName().toString().equals(method) ){
			//&& n.getMethod().getDeclaringClass().getName().getClassName().toString().equals(cla)) {
				IR ir = cache.getIR(n.getMethod(), Everywhere.EVERYWHERE);

				if (ir == null||ir.getMethod() instanceof SummarizedMethod) {
					return null;
				}
				IBytecodeMethod bytemethod = (IBytecodeMethod) ir.getMethod();
				Iterator<SSAInstruction> instruction = ir.iterateAllInstructions();
				// 遍历方法中的所有指令
				while (instruction.hasNext()) {
					SSAInstruction ins = instruction.next();
					instructions.add(ins);
				}

				int tm = instructions.get(instructions.size() - 1).iindex;
				int k = 1;
				while (tm == -1) {
					tm = instructions.get(instructions.size() - k).iindex;
					k++;
				}
				for (int in = 0; in <= tm; in++) {
					bytecodeIndex = bytemethod.getBytecodeIndex(in);
					sourceLineNum = bytemethod.getLineNumber(bytecodeIndex);
					// int sourceposition=method.getSourcePosition(ins.iindex);
					if (!linenum.containsKey(sourceLineNum)) {
						linenum.put(sourceLineNum, line);
						line++;
					}
				}
				Iterator<Integer> set = linenum.keySet().iterator();
				while (set.hasNext()) {
					list = new ArrayList<String>();
					map.put(linenum.get(set.next()), list);
				}
			//	while (l < instructions.size()) {
//					if (instructions.get(l) instanceof SSAInvokeInstruction
//							&&!((SSAInvokeInstruction)instructions.get(l)).isSpecial()) {
//						SSAInvokeInstruction ssai=((SSAInvokeInstruction)instructions.get(l));
//							if (ssai.getCallSite().getDeclaredTarget().getName().toString().equals("wait")
//									|| ssai.getCallSite().getDeclaredTarget().getName().toString().equals("notify")
//									|| ssai.getCallSite().getDeclaredTarget().getName().toString().equals("notifyAll"))
//								return "condi";
//					}
//					System.out.println(instructions.get(l));
//					l++;
				//}
				while (l < instructions.size()) {
						sb.append(geteffect(instructions.get(l), n, bytemethod));
				}
				return sb.toString();
			}

		}
		return null;

	}


	// 传入一个指令数组或是arraylist
	public String geteffect(SSAInstruction ins, CGNode n, IBytecodeMethod method) throws InvalidClassFileException {
		int if_index = 0;
		int else_index = Integer.MAX_VALUE;
		int bytecodeIndex, sourceLineNum;
		StringBuffer sb = new StringBuffer();
		if (ins instanceof SSAInvokeInstruction) {
			SSAInvokeInstruction ssai = (SSAInvokeInstruction) ins;
			
			if (ssai.getCallSite().getDeclaredTarget().getDeclaringClass().getName().getPackage().toString()
					.equals("java/util")) {
				if (analysisPrimordial(ssai.getDeclaredTarget().getName().toString(), n)) {
					sb.append(RWSign.READ_SIGN);
					bytecodeIndex = method.getBytecodeIndex(ins.iindex);
					sourceLineNum = method.getLineNumber(bytecodeIndex);
					map.get(linenum.get(sourceLineNum)).add(RWSign.READ_SIGN);
					map.put(linenum.get(sourceLineNum), map.get(linenum.get(sourceLineNum)));
				} else {
					sb.append(RWSign.WRITE_SIGN);
					bytecodeIndex = method.getBytecodeIndex(ins.iindex);
					sourceLineNum = method.getLineNumber(bytecodeIndex);
					map.get(linenum.get(sourceLineNum)).add(RWSign.WRITE_SIGN);
					map.put(linenum.get(sourceLineNum), map.get(linenum.get(sourceLineNum)));
				}
			} else {
				if (analysisInvokeMethod(ssai.getDeclaredTarget().getName().toString(), n)) {
					sb.append(RWSign.READ_SIGN);
					bytecodeIndex = method.getBytecodeIndex(ins.iindex);
					sourceLineNum = method.getLineNumber(bytecodeIndex);
					map.get(linenum.get(sourceLineNum)).add(RWSign.READ_SIGN);
					map.put(linenum.get(sourceLineNum), map.get(linenum.get(sourceLineNum)));
				} else {
					sb.append(RWSign.WRITE_SIGN);
					bytecodeIndex = method.getBytecodeIndex(ins.iindex);
					sourceLineNum = method.getLineNumber(bytecodeIndex);
					map.get(linenum.get(sourceLineNum)).add(RWSign.WRITE_SIGN);
					map.put(linenum.get(sourceLineNum), map.get(linenum.get(sourceLineNum)));
				}

			}
			deep = 0;
		} else if (ins instanceof SSABinaryOpInstruction) {
			bytecodeIndex = method.getBytecodeIndex(ins.iindex);
			sourceLineNum = method.getLineNumber(bytecodeIndex);
			map.get(linenum.get(sourceLineNum)).add(RWSign.WRITE_SIGN);
			map.put(linenum.get(sourceLineNum), map.get(linenum.get(sourceLineNum)));
			sb.append(RWSign.WRITE_SIGN);
		} else if (ins instanceof SSANewInstruction || ins instanceof SSACheckCastInstruction) {
			bytecodeIndex = method.getBytecodeIndex(ins.iindex);
			sourceLineNum = method.getLineNumber(bytecodeIndex);
			map.get(linenum.get(sourceLineNum)).add(RWSign.READ_SIGN);
			map.put(linenum.get(sourceLineNum), map.get(linenum.get(sourceLineNum)));
			sb.append(RWSign.READ_SIGN);
		} else if (ins instanceof SSAFieldAccessInstruction) {
			if (ins instanceof SSAPutInstruction) {
				sb.append(RWSign.WRITE_SIGN);
				bytecodeIndex = method.getBytecodeIndex(ins.iindex);
				sourceLineNum = method.getLineNumber(bytecodeIndex);
				map.get(linenum.get(sourceLineNum)).add(RWSign.WRITE_SIGN);
				map.put(linenum.get(sourceLineNum), map.get(linenum.get(sourceLineNum)));
			} else {
				sb.append(RWSign.READ_SIGN);
				bytecodeIndex = method.getBytecodeIndex(ins.iindex);
				sourceLineNum = method.getLineNumber(bytecodeIndex);
				map.get(linenum.get(sourceLineNum)).add(RWSign.READ_SIGN);
				map.put(linenum.get(sourceLineNum), map.get(linenum.get(sourceLineNum)));
			}
		} else if (ins instanceof SSAConditionalBranchInstruction) {
			SSAConditionalBranchInstruction ssac = (SSAConditionalBranchInstruction) ins;
			bytecodeIndex = method.getBytecodeIndex(ins.iindex);
			sourceLineNum = method.getLineNumber(bytecodeIndex);
			map.get(linenum.get(sourceLineNum)).add(RWSign.CONDITION_SIGN);
			map.put(linenum.get(sourceLineNum), map.get(linenum.get(sourceLineNum)));
			sb.append(RWSign.CONDITION_SIGN);
			if_index = ssac.getTarget();
			l++;
			while (l < instructions.size() && instructions.get(l).iindex <= if_index) {
				sb.append(geteffect(instructions.get(l), n, method));
			}
			l--;
			bytecodeIndex = method.getBytecodeIndex(if_index - 1);
			sourceLineNum = method.getLineNumber(bytecodeIndex);
			map.get(linenum.get(sourceLineNum)).add(RWSign.END_CON_SIGN);
			map.put(linenum.get(sourceLineNum), map.get(linenum.get(sourceLineNum)));
			sb.append(RWSign.END_CON_SIGN);
		} else if (ins instanceof SSAGotoInstruction) {
			SSAGotoInstruction ssag = (SSAGotoInstruction) ins;
			l++;
			if (l < instructions.size() && instructions.get(l) instanceof SSAMonitorInstruction) {
				sb.append(RWSign.READ_SIGN);
				else_index = ssag.getTarget();
				bytecodeIndex = method.getBytecodeIndex(else_index);
				sourceLineNum = method.getLineNumber(bytecodeIndex);
				map.get(linenum.get(sourceLineNum)).add("M");
				map.put(linenum.get(sourceLineNum), map.get(linenum.get(sourceLineNum)));
				l--;
			} else {
				sb.append(RWSign.ELSE_SIGN);
				else_index = ssag.getTarget();
				bytecodeIndex = method.getBytecodeIndex(ins.iindex);
				sourceLineNum = method.getLineNumber(bytecodeIndex);
				map.get(linenum.get(sourceLineNum)).add(RWSign.LOOP_SIGN);
				map.put(linenum.get(sourceLineNum), map.get(linenum.get(sourceLineNum)));
				l++;
				while (l < instructions.size() && instructions.get(l).iindex <= else_index) {
					sb.append(geteffect(instructions.get(l), n, method));
				}
				l--;
				if(else_index>1) {
					bytecodeIndex = method.getBytecodeIndex(else_index - 1);
				sourceLineNum = method.getLineNumber(bytecodeIndex);
				}
				
				map.get(linenum.get(sourceLineNum)).add(RWSign.END_CON_SIGN);
				map.put(linenum.get(sourceLineNum), map.get(linenum.get(sourceLineNum)));
				sb.append(RWSign.END_CON_SIGN);
			}

		} else if (ins instanceof SSAMonitorInstruction) {
			bytecodeIndex = method.getBytecodeIndex(ins.iindex);
			sourceLineNum = method.getLineNumber(bytecodeIndex);
			map.get(linenum.get(sourceLineNum)).add("M");
			map.put(linenum.get(sourceLineNum), map.get(linenum.get(sourceLineNum)));
		} else if (ins instanceof SSAGetCaughtExceptionInstruction || ins instanceof SSAPhiInstruction
				|| ins instanceof SSAPiInstruction) {

		} else if(ins instanceof SSAReturnInstruction){
			SSAReturnInstruction ssare = (SSAReturnInstruction) ins;
			if(!ssare.returnsVoid()) {
				sb.append(RWSign.READ_SIGN);
			}
		}else {
			bytecodeIndex = method.getBytecodeIndex(ins.iindex);
			sourceLineNum = method.getLineNumber(bytecodeIndex);
			map.get(linenum.get(sourceLineNum)).add(RWSign.READ_SIGN);
			map.put(linenum.get(sourceLineNum), map.get(linenum.get(sourceLineNum)));
			sb.append(RWSign.READ_SIGN);
		}
		l++;
		return sb.toString();
	}

	public boolean analysisPrimordial(String m, CGNode n) {

		IAnalysisCacheView cache = new AnalysisCacheImpl();
		Iterator<CGNode> outer = cg.getSuccNodes(n);

		int rcout = 0;
		int icout = 0;
		if (deep < 2) {
			deep++;
			while (outer.hasNext()) {
				CGNode me = outer.next();
				if (me.getMethod().getName().toString().equals(m)) {
					IR lr = cache.getIR(me.getMethod());
					Iterator<SSAInstruction> instruction2 = lr.iterateAllInstructions();
					while (instruction2.hasNext()) {
						icout++;
						SSAInstruction ins2 = instruction2.next();
						if (ins2 instanceof SSAPutInstruction || ins2 instanceof SSAAbstractBinaryInstruction) {
							return false;
						} else if (ins2 instanceof SSAMonitorInstruction) {
							return true;
						} else if (ins2 instanceof SSAConversionInstruction || ins2 instanceof SSAGetInstruction
								|| ins2 instanceof SSAReturnInstruction) {
							rcout++;
						} else if (ins2 instanceof SSAInvokeInstruction) {
							// 判断是否是构造函数
							if (((SSAAbstractInvokeInstruction) ins2).getDeclaredTarget().isInit()) {
								rcout++;
							} else if (analysisPrimordial(
									((SSAAbstractInvokeInstruction) ins2).getDeclaredTarget().getName().toString(),
									me)) {
								rcout++;
							} else {
								return false;
							}
						}
					}
					if (icout == rcout) {
						return true;
					}
				}
			}
		}
		return true;
	}

	public boolean analysisInvokeMethod(String m, CGNode n) {
		
		IAnalysisCacheView cache = new AnalysisCacheImpl();
		Iterator<CGNode> outer = cg.getSuccNodes(n);
		int rcout = 0;
		int icout = 0;
		if(invo<4) {
		invo++;
		while (outer.hasNext()) {
			CGNode me = outer.next();
			if (me.getMethod().getName().toString().equals(m)) {
				IR lr = cache.getIR(me.getMethod());
				if (lr == null) {
					return false;
				}
				Iterator<SSAInstruction> instruction2 = lr.iterateAllInstructions();
				while (instruction2.hasNext()) {
					icout++;
					SSAInstruction ins2 = instruction2.next();
					if (ins2 instanceof SSAPutInstruction || ins2 instanceof SSAAbstractBinaryInstruction) {
						return false;
					} else if (ins2 instanceof SSAMonitorInstruction) {
						return true;
					} else if (ins2 instanceof SSAConversionInstruction || ins2 instanceof SSAGetInstruction
							|| ins2 instanceof SSAReturnInstruction) {
						rcout++;
					} else if (ins2 instanceof SSAInvokeInstruction) {
						// 判断是否是构造函数
						if (((SSAAbstractInvokeInstruction) ins2).getDeclaredTarget().isInit()) {
							rcout++;
						} else if (analysisInvokeMethod(
								((SSAAbstractInvokeInstruction) ins2).getDeclaredTarget().getName().toString(), me)) {
							rcout++;
						} else {
							return false;
						}
					}
				}
				if (icout == rcout) {
					return true;
				}
			}
		}
		}
		return true;
	}

	public Map<Integer, Integer> getlinemap() {
		return linenum;
	}

	public List<Integer> getwritelist() {
		return writlist;
	}

	//C是if语句，E是其他条件语句
	public String getsToMethod() {
		StringBuffer sb = new StringBuffer();
		
		for (int s : map.keySet()) {
			if (map.get(s).contains("C")) {
				if (map.get(s).contains("G")) {
					sb.append("E");
				} else {
					sb.append("C");
				}
			} else if (map.get(s).contains("T")) {
				if (map.get(s).contains("W")) {
					sb.append("W");
					sb.append("T");
				} else {
					sb.append("R");
					sb.append("T");
				}

			} else if (map.get(s).contains("W")) {
				sb.append("W");
			} else if (map.get(s).contains("R")) {
				sb.append("R");
			} 
//			else {
//				sb.append("A");
//			}

		}

		return sb.toString();
	}

	public String getsToBlock() {
		//System.out.println(map);
		StringBuffer sb = new StringBuffer();
		for (int s : map.keySet()) {
			if (map.get(s).contains("M")) {
				int i = 0;
				Iterator<String> tm = map.get(s).iterator();
				while (tm.hasNext()) {
					if (tm.next() == "M")
						i++;
				}
				if (i > 1) {
					sb.append("M");
				} else if (map.get(s).contains("W")) {
					sb.append("M");
					sb.append("W");
				} else if (map.get(s).contains("R")) {
					sb.append("M");
					sb.append("R");
				}
			} else if (map.get(s).contains("C")) {
				if (map.get(s).contains("G")) {
					sb.append("E");
				} else {
					sb.append("C");
				}
			} else if (map.get(s).contains("T")) {
				if (map.get(s).contains("W")) {
					sb.append("W");
					sb.append("T");
				} else {
					sb.append("R");
					sb.append("T");
				}

			} else if (map.get(s).contains("W")) {
				sb.append("W");
			} else {
				sb.append("R");
			}

		}
		
		if (sb.length() != 0) {
			//删除方法最后的return
			sb.deleteCharAt(sb.length() - 1);
		}
		return sb.toString();
	}

	public String makeReToMethod() {
		StringBuffer sb = new StringBuffer();
		List<Character> tmp = new ArrayList<Character>();
		String s = getsToMethod();
		char[] c = s.toCharArray();
		for (int i = 0; i < c.length; i++) {
			if (c[i] == 'C' || c[i] == 'E') {
				while (i < c.length && c[i] != 'T') {
					tmp.add(c[i]);
					i++;
				}
				if (tmp.contains('W')) {
					sb.append("W");
				} else {
					sb.append("R");
				}
			} else if (c[i] == 'W') {
				sb.append("W");
			} else if (c[i] == 'R') {
				sb.append("R");
			} else {
				sb.append("A");
			}

		}
		return sb.toString();
	}

	public String makeReToBlock() {
		StringBuffer sb = new StringBuffer();
		List<Character> tmp = new ArrayList<Character>();
		String s = getsToBlock();
		char[] c = s.toCharArray();
		for (int i = 0; i < c.length; i++) {
			if (c[i] == 'M') {
				sb.append("M");
			} else if (c[i] == 'C' || c[i] == 'E') {
				while (i+1<c.length&&c[++i] != 'T') {
					tmp.add(c[i]);
				}
				if (tmp.contains('W')) {
					sb.append("W");
				} else {
					sb.append("R");
				}
			} else if (c[i] == 'W') {
				sb.append("W");
			} else {
				sb.append("R");
			}
		}
		c = sb.toString().toCharArray();
		int t = c.length;
		int y = 0;
		int bindex = 0;
		int aindex = 0;
		while (t-1>=0&&c[t - 1] != 'M') {
			t--;
			aindex++;
		}
		while (y<c.length&&c.length!=0&&c[y] != 'M') {
			y++;
			bindex++;
		}
		for (int i = 0; i < aindex; i++) {
			sb.deleteCharAt(sb.length() - 1);
		}
		for (int i = 0; i < bindex; i++) {
			if(sb.length()!=0) {
				sb.deleteCharAt(0);
			}
			
		}
		return sb.toString();

	}

	public boolean checkRe() {
		Iterator<Integer> it = rl.iterator();
		while (it.hasNext()) {
			if (wl.contains(it.next())) {
				return true;
			}
		}

		return false;
	}

}
