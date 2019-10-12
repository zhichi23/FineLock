package rwlockrefactoring.refactoring;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.SynchronizedStatement;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jface.text.Document;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.CallGraphBuilderCancelException;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.shrikeCT.InvalidClassFileException;

import rwlockrefactoring.analysis.LockSet;
import rwlockrefactoring.analysis.MakeCallGraph;
import rwlockrefactoring.analysis.SideEffectAnalysis;
import rwlockrefactoring.nfa.BlockString;
import rwlockrefactoring.nfa.DFA;
import rwlockrefactoring.nfa.MethodString;
import rwlockrefactoring.nfa.NFA;
import rwlockrefactoring.util.Count;
import rwlockrefactoring.util.MessageBox;
import rwlockrefactoring.util.RWSign;
import rwlockrefactoring.util.RWString;

/**
 * This class is the refactored action class The preview of the refactoring is
 * also done through this class
 * 
 * @author Shao
 * @version 4.0_01
 * 
 */
public class RWLockRefactoring extends Refactoring {

	// 所有的重构变化
	List<Change> changeManager = new ArrayList<Change>();
	// 所有需要修改的JavaElement
	List<IJavaElement> compilationUnits = new ArrayList<IJavaElement>();

	// 所要重构程序的路径
	static IPath filename;

	NFA down_nfa_method = new NFA(RWString.DOWNGRADE_LOCK_METHOD);
	NFA up_nfa_method = new NFA(RWString.UPGRADE_LOCK_METHOD);
	NFA down_nfa_block = new NFA(RWString.DOWNGRADE_LOCK_BLOCK);
	NFA up_nfa_block = new NFA(RWString.UPGRADE_LOCK_BLOCK);

	MakeCallGraph cg;
	CallGraph callgraph;
	ASTRewrite rewrite;
	// parameter
	boolean synMethod = false;
	boolean synBlock = false;

	Count count=new Count();
	MessageBox mb=new MessageBox();

	Map<String, Integer> countmap = new HashMap<String, Integer>();

	/**
	 * construction method
	 * 
	 * @param element
	 * @throws IOException
	 * @throws CallGraphBuilderCancelException
	 * @throws IllegalArgumentException
	 * @throws ClassHierarchyException
	 */
	public RWLockRefactoring(Object element) {
		filename = ((IJavaElement) element).getJavaProject().getProject().getLocation();
		cg = new MakeCallGraph(filename.toString());
		findAllCompilationUnits(element);
	}

	/**
	 * FinalConditions
	 * 
	 * @throws JavaModelException
	 * 
	 */
	@Override
	public RefactoringStatus checkFinalConditions(IProgressMonitor pm) throws JavaModelException {
		// TODO Auto-generated method stub
		try {
			collectChanges();
		} catch (IllegalArgumentException | CallGraphBuilderCancelException | ClassHierarchyException
				| UnsupportedOperationException | IOException | InvalidClassFileException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if (changeManager.size() == 0)
			return RefactoringStatus.createFatalErrorStatus("No synchronized methods/blocks found!");
		else
			return RefactoringStatus.createInfoStatus("Final condition has been checked");

	}

	/**
	 * InitialConditions of the refactoring
	 */
	@Override
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm)
			throws CoreException, OperationCanceledException {
		// has no initial　condition
		return RefactoringStatus.createInfoStatus("Initial Condition is OK!");
	}

	@Override
	public Change createChange(IProgressMonitor arg0) {
		// TODO Auto-generated method stub
		Change[] changes = new Change[changeManager.size()];
		System.arraycopy(changeManager.toArray(), 0, changes, 0, changeManager.size());
		CompositeChange change = new CompositeChange("refactor rwLock", changes);
		return change;
	}

	/**
	 * This method must have a return value, otherwise the finish button is not
	 * available
	 */
	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return "ss";
	}

	/**
	 * 待修改 
	 * @param project
	 */
	
	private void findAllCompilationUnits(Object project) {
		if (project instanceof ICompilationUnit) {
			compilationUnits.add(((ICompilationUnit) project));
		} else if (project instanceof IPackageFragment) {
			try {
				for (ICompilationUnit unit : ((IPackageFragment) project).getCompilationUnits()) {
					compilationUnits.add(unit);
				}
			} catch (JavaModelException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else if (project instanceof IJavaElement) {
			try {
//				if(project instanceof JavaProject){
//					JavaProject jp=(JavaProject)project;
//					IJavaElement[] je=jp.getChildren();
//					for(IJavaElement jee:je) {
//						
//						if(jee instanceof IPackageFragment) {
//						for (IJavaElement ele : ((IPackageFragment)jee).getChildren()) {
//							if (ele instanceof IPackageFragment) {
//								IPackageFragment fragment = (IPackageFragment) ele;
//								// 遍历所有类
//								for (ICompilationUnit unit : fragment.getCompilationUnits()) {
//									compilationUnits.add(unit);
//								}
//							}
//						}
//					}}
//				}
				IJavaElement element1 = (IJavaElement) project;
				//if (element1.getElementName().equals("src")) {
					
					IPackageFragmentRoot root = (IPackageFragmentRoot) element1;
					for (IJavaElement ele : root.getChildren()) {
						if (ele instanceof IPackageFragment) {
							IPackageFragment fragment = (IPackageFragment) ele;
							// 遍历所有类
							for (ICompilationUnit unit : fragment.getCompilationUnits()) {
								compilationUnits.add(unit);
							}
						}
					}

			} catch (JavaModelException e) {
				e.printStackTrace();
			}
		}else {
			try {
				IJavaElement element1 = (IJavaElement) project;
				//if (element1.getElementName().equals("src")) {
					
					IPackageFragmentRoot root = (IPackageFragmentRoot) element1;
					for (IJavaElement ele : root.getChildren()) {
						if (ele instanceof IPackageFragment) {
							IPackageFragment fragment = (IPackageFragment) ele;
							// 遍历所有类
							for (ICompilationUnit unit : fragment.getCompilationUnits()) {
								compilationUnits.add(unit);
							}
						}
					}

			} catch (JavaModelException e) {
				e.printStackTrace();
			}
			
		}

	}

	/**
	 * 
	 * @throws JavaModelException
	 * @throws InvalidClassFileException
	 * @throws IOException
	 * @throws UnsupportedOperationException
	 * @throws ClassHierarchyException
	 * @throws CallGraphBuilderCancelException
	 * @throws IllegalArgumentException
	 */
	private void collectChanges() throws JavaModelException, IllegalArgumentException, CallGraphBuilderCancelException,
			ClassHierarchyException, UnsupportedOperationException, IOException, InvalidClassFileException {
		// 构建调用图
		callgraph = cg.callgraph();
		for (IJavaElement element : compilationUnits) {

			// 创建一个document(jface)
			ICompilationUnit cu = (ICompilationUnit) element;
			String source = cu.getSource();
			Document document = new Document(source);
			// 创建AST
			ASTParser parser = ASTParser.newParser(AST.JLS11);
			parser.setSource(cu);
			CompilationUnit astRoot = (CompilationUnit) parser.createAST(null);
			rewrite = ASTRewrite.create(astRoot.getAST());
			// 记录更改
			astRoot.recordModifications();

			// 找到synchronized方法
			// List<MethodDeclaration> methods = new ArrayList<MethodDeclaration>();
			List<TypeDeclaration> types = new ArrayList<TypeDeclaration>();
			// getMethods(astRoot.getRoot(), methods);
//			for(CGNode g:callgraph) {
//				System.out.println(g);
//			}
			getTypes(astRoot.getRoot(), types);
			for (TypeDeclaration ty : types) {
				countmap.put(ty.getName().toString(), 0);
			}

			for (TypeDeclaration ty : types) {
				collectChanges(astRoot, ty);
			}
			// TODO:待解决MalformedTreeException
			try {
				TextEdit edits = astRoot.rewrite(document, cu.getJavaProject().getOptions(true));
				TextFileChange change = new TextFileChange("", (IFile) cu.getResource());
				change.setEdit(edits);
				changeManager.add(change);
			} catch (MalformedTreeException e) {
				e.getMessage();
			}

		}
		mb.print_num(count);

	}

	/**
	 * 获得所有类/接口
	 * 
	 * @param cuu
	 * @param types
	 */
	@SuppressWarnings("rawtypes")
	private void getTypes(ASTNode cuu, final List types) {
		cuu.accept(new ASTVisitor() {
			@SuppressWarnings("unchecked")
			public boolean visit(TypeDeclaration node) {
				types.add(node);
				return false;
			}
		});
	}

	@SuppressWarnings("unchecked")
	private boolean collectChanges(CompilationUnit root, TypeDeclaration types) throws IllegalArgumentException, CallGraphBuilderCancelException,
			ClassHierarchyException, UnsupportedOperationException, IOException, InvalidClassFileException {
		AST ast = types.getAST();
		ImportDeclaration id1 = ast.newImportDeclaration();
		ImportDeclaration id2 = ast.newImportDeclaration();

		id1.setName(ast.newName(new String[] { "java", "util", "concurrent", "locks", "ReentrantReadWriteLock" }));
		id2.setName(ast.newName(new String[] { "java", "util", "concurrent", "locks", "Lock" }));
		RefactoringUtil rutil=new RefactoringUtil();
		List<String> lockex = new LinkedList<String>();
		Map<MethodDeclaration, String> inmap = new HashMap<MethodDeclaration, String>();
		// 获取当前类的所有锁句柄
		List<MethodDeclaration> absMethod=new ArrayList<MethodDeclaration>();
		rutil.getlockex(lockex, inmap, types,absMethod);
		LockSet ls = new LockSet(cg.pointer(), cg.cha());
		ls.lockmap(lockex, types.getName().toString());
		MethodDeclaration[] ms = types.getMethods();
		List<String> tmp = new LinkedList<String>();
		Map<String, String> result = new HashMap<String, String>();
		 int zanshi=0;
		LockRefactoring lf;
		if (synMethod) {
			rutil.decalock(ast, types, ls, tmp, result);
			for (MethodDeclaration m : ms) {
				for (int i = 0; i < m.modifiers().size(); i++) {
					if (m.modifiers().get(i).toString().equals("synchronized")) {
						if(m.getBody().statements().size()==0) {
							count.sy_can_not1++;
							break;
						}
						lf=new RefactoringToMethod(result);
						count.sy_num++;
						SideEffectAnalysis sda1 = new SideEffectAnalysis(callgraph);
						countmap.put(types.getName().toString(), countmap.get(types.getName().toString()) + 1);
						String rws = sda1.sideEffect(m.getName().toString(), types.getName().toString(),
								m.parameters());
						if(rws=="condi") {
							count.sy_can_not2++;
							break;
						}
						String rws1 = sda1.getsToMethod();
						String rws2 = sda1.makeReToMethod();
						MethodString mstring=new MethodString(rws1, rws2);
						
						//System.out.println("rws1"+rws1);
						//System.out.println("rws2"+rws2);
						DFA down_dfa = new DFA(rws1);
						rutil.addImport(root.imports(), id1);
						rutil.addImport(root.imports(), id2);
						m.modifiers().remove(i);
//						if(m.getName().toString().equals("testWrite")) {
//							lf.refactoring_write(ast, m, inmap.get(m));
//						}else {
//							lf.refactoring_down(ast, m, inmap.get(m));
//						}
						
						if (rws==null||rws1.length()==0) {
							if(zanshi==0) {rutil.addlock(ast,types,"nulock",true);}
							zanshi++;
							lf.refactoring_null(ast, m);
							//writelockToMethod(ast, m, inmap.get(m), result);
							//count.sy_can_not1++;
							count.sy_write_num++;
						}
						else { 
							String s=mstring.match();
							if (s=="D") {
							lf.refactoring_down(ast, m, inmap.get(m));
							count.sy_down_num++;
						} else if (s=="U") {
							lf.refactoring_up(ast, m,inmap.get(m));
							count.sy_up_num++;
						} else if (s=="DS") {
							lf.refactoring_downs(ast, m, inmap.get(m), rws2);
							count.sy_down_num++;
						}else if(s=="US") {
							lf.refactoring_ups(ast, m,inmap.get(m),rws2);
							count.sy_up_num++;
						}else if (!rws1.contains(RWSign.WRITE_SIGN)) {
							lf.refactoring_read(ast, m, inmap.get(m));
							count.sy_read_num++;
						} else {
							
							lf.refactoring_write(ast, m, inmap.get(m));
							count.sy_write_num++;

						}}

					}
				}
			}
		}
		if (synBlock) {
			rutil.decalock(ast, types, ls, tmp, result);
			if(!types.isInterface()) {
			for (MethodDeclaration m : ms) {
				if(!absMethod.contains(m)) {
				for (int b = 0; b < m.getBody().statements().size(); b++) {
					if (m.getBody().statements().get(b) instanceof SynchronizedStatement) {
						lf=new RefactoringToBlock(result, b);
						count.bl_num++;
						SynchronizedStatement sst = (SynchronizedStatement) m.getBody().statements().get(b);
						String tolock = null;
						if (sst.getExpression().toString().equals("getClass()")) {
							tolock = "static";
						} else {
							tolock = sst.getExpression().toString();
						}
						SideEffectAnalysis sda2 = new SideEffectAnalysis(callgraph);
						String rws = sda2.sideEffect(m.getName().toString(), types.getName().toString(),
								m.parameters());
						if(rws=="condi") {
							count.sy_can_not2++;
							break;
						}
						String rws1 = sda2.getsToBlock();
						String rws2 = sda2.makeReToBlock();
						rutil.addImport(root.imports(), id1);
						rutil.addImport(root.imports(), id2);
						BlockString bstring=new BlockString(rws1,rws2);
						if (rws1.length()==0||rws==null) {
							//writelockToBlock(ast, m, b, tolock, result);
							lf.refactoring_null(ast, m);
							count.bl_write_num++;
						} else {
							String s=bstring.match();
						if (s=="D") {
							lf.refactoring_down(ast, m, inmap.get(m));
							count.bl_down_num++;
						} else if(s=="DS") {
							lf.refactoring_null(ast, m);
							//lf.refactoring_downs(ast, m, inmap.get(m),rws2);
						}else if (s=="U") {
							lf.refactoring_null(ast, m);
							//lf.refactoring_up(ast, m, inmap.get(m));
							count.bl_up_num++;
						} else if(s=="US") {
							lf.refactoring_null(ast, m);
							//lf.refactoring_ups(ast, m, inmap.get(m),rws2);
						}else if (!rws2.contains(RWSign.WRITE_SIGN)) {
							lf.refactoring_null(ast, m);
							//lf.refactoring_read(ast, m, inmap.get(m));
							count.bl_read_num++;
						} else {
							//System.out.println(types.getName().toString()+"  "+m.getName());
							lf.refactoring_null(ast, m);
							//lf.refactoring_write(ast, m, inmap.get(m));
							count.bl_write_num++;
						}}
					}
				}}
			}
			}
		}
		return true;

	}



	public void setsynMethod(boolean n) {
		synMethod = n;
	}

	public void setsynBlock(boolean n) {
		synBlock = n;
	}

}
