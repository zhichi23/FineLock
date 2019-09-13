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
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier.ModifierKeyword;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.SynchronizedStatement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jface.text.Document;
import org.eclipse.ltk.core.refactoring.*;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;

import com.ibm.wala.classLoader.IField;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.CallGraphBuilderCancelException;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import org.eclipse.jdt.core.dom.Statement;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.types.MethodReference;

import rwlockrefactoring.analysis.LockSet;
import rwlockrefactoring.analysis.MakeCallGraph;
import rwlockrefactoring.analysis.SideEffectAnalysis;
import rwlockrefactoring.nfa.DFA;
import rwlockrefactoring.nfa.NFA;
import rwlockrefactoring.util.LockSign;
import rwlockrefactoring.util.RWSign;
import rwlockrefactoring.util.RWString;

/**
 * This class is the refactored action class The preview of the refactoring is
 * also done through this class
 * 
 * @author Shao
 * @version 3.0
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

	// 统计
	int sy_num = 0;
	int sy_up_num = 0;
	int sy_down_num = 0;
	int sy_read_num = 0;
	int sy_write_num = 0;
	int bl_num = 0;
	int bl_up_num = 0;
	int bl_down_num = 0;
	int bl_read_num = 0;
	int bl_write_num = 0;

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
		// has no initialcondition
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
	 * 
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
				IJavaElement element1 = (IJavaElement) project;
				if (element1.getElementName().equals("java")) {
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
				} else {
					// 遍历工程下的所有文件
					for (IJavaElement element : ((IJavaProject) project).getChildren()) {
						// 遍历src下面的所有包
						if (element.getElementName().equals("src")) {
							IPackageFragmentRoot root = (IPackageFragmentRoot) element;
							for (IJavaElement ele : root.getChildren()) {
								if (ele instanceof IPackageFragment) {
									IPackageFragment fragment = (IPackageFragment) ele;
									// 遍历所有类
									for (ICompilationUnit unit : fragment.getCompilationUnits()) {
										compilationUnits.add(unit);
									}
								}
							}
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
			getTypes(astRoot.getRoot(), types);
			for (TypeDeclaration ty : types) {
				countmap.put(ty.getName().toString(), 0);
			}

			for (TypeDeclaration ty : types) {
				List<String> lockex = new LinkedList<String>();
				Map<MethodDeclaration, String> inmap = new HashMap<MethodDeclaration, String>();
				//获取当前类的所有锁句柄
				getlockex(lockex, inmap, ty);
				LockSet ls = new LockSet(cg.pointer(), cg.cha());
				ls.lockmap(lockex, ty.getName().toString());
				collectChanges(astRoot, ty, ls, inmap);

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
		print_num();

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
	private boolean collectChanges(CompilationUnit root, TypeDeclaration types, LockSet ls,
			Map<MethodDeclaration, String> inmap) throws IllegalArgumentException, CallGraphBuilderCancelException,
			ClassHierarchyException, UnsupportedOperationException, IOException, InvalidClassFileException {
		AST ast = types.getAST();
		ImportDeclaration id1 = ast.newImportDeclaration();
		ImportDeclaration id2 = ast.newImportDeclaration();

		id1.setName(ast.newName(new String[] { "java", "util", "concurrent", "locks", "ReentrantReadWriteLock" }));
		id2.setName(ast.newName(new String[] { "java", "util", "concurrent", "locks", "Lock" }));

		MethodDeclaration[] ms = types.getMethods();
		List<String> tmp = new LinkedList<String>();
		Map<String, String> result = new HashMap<String, String>();
		if (synMethod) {
			decalock(ast, types, ls, tmp, result);
			for (MethodDeclaration m : ms) {
				for (int i = 0; i < m.modifiers().size(); i++) {
					if (m.modifiers().get(i).toString().equals("synchronized")) {
						SideEffectAnalysis sda1 = new SideEffectAnalysis(callgraph);
						countmap.put(types.getName().toString(), countmap.get(types.getName().toString()) + 1);
						String rws = sda1.sideEffect(m.getName().toString(), types.getName().toString(),
								m.parameters());
						String rws1 = sda1.getsToMethod();
						String rws2 = sda1.makeReToMethod();
						DFA down_dfa = new DFA(rws1);
						addImport(root.imports(), id1);
						addImport(root.imports(), id2);
						m.modifiers().remove(i);
						if (rws1 == null || rws == null) {
							writelockToMethod(ast, m, inmap.get(m), result);
							sy_write_num++;
						} else if (down_dfa.makedfa()) {
							downgradeToMethod(ast, m, inmap.get(m), result);
							sy_down_num++;
						} else if (up_nfa_method.recognizes(rws2)) {
							upgradeToMethod(ast, m, inmap.get(m), result, rws2);
//						if (upgrade(ast, types, m, 1, bl, rws2)) {
//							sy_up_num++;
//						} else {
//							sy_write_num++;
//						}

						} else if (down_nfa_method.recognizes(rws2)) {
							downgradeToMethod2(ast, m, inmap.get(m), result, rws2);
//						if (downgrade(ast, types, m, 2, bl, rws2)) {
//							sy_down_num++;
//						} else {
//							sy_write_num++;
//						}
						} else if (!rws1.contains(RWSign.WRITE_SIGN)) {
							readlockToMethod(ast, m, inmap.get(m), result);
							sy_read_num++;
						} else {
							writelockToMethod(ast, m, inmap.get(m), result);
							sy_write_num++;

						}

					}
				}
			}
		}
		if (synBlock) {
			decalock(ast, types, ls, tmp, result);
			for (MethodDeclaration m : ms) {
				for (int b = 0; b < m.getBody().statements().size(); b++) {
					if (m.getBody().statements().get(b) instanceof SynchronizedStatement) {
						SynchronizedStatement sst = (SynchronizedStatement) m.getBody().statements().get(b);
						String tolock = null;
						if (sst.getExpression().toString().equals("getClass()")) {
							tolock = "static";
						} else {
							tolock = sst.getExpression().toString();
						}
						SideEffectAnalysis sda2 = new SideEffectAnalysis(callgraph);
						String rws2 = sda2.sideEffect(m.getName().toString());
						addImport(root.imports(), id1);
						addImport(root.imports(), id2);

						// writelockToBlock(ast,m,ls,tmp, b,sst.getExpression().toString(),result);
						if (rws2 == null) {
							writelockToBlock(ast, m, b, tolock, result);
							bl_write_num++;
						} else if (down_nfa_block.recognizes(sda2.makeReToBlock())) {
							if (sda2.makeReToBlock() == null) {
								downgradeToBlock(ast, m, b, tolock, result);
							} else {
								downgradeToBlock2(ast, m, b, tolock, result, sda2.makeReToBlock());
							}
							bl_down_num++;
						} else if (up_nfa_block.recognizes(sda2.makeReToBlock())) {
							upgradeToBlock(ast, m, b, tolock, result, sda2.makeReToBlock());
							bl_up_num++;
						} else if (!rws2.contains(RWSign.WRITE_SIGN)) {
							readlockToBlock(ast, m, b, tolock, result);
							bl_read_num++;
						} else {
							writelockToBlock(ast, m, b, tolock, result);
							bl_write_num++;
						}
					}
				}
			}
		}
		return true;

	}

	/**
	 * set modifiers of the field
	 * 
	 * @param f
	 * @param ast
	 */
	@SuppressWarnings("unchecked")
	private void aadfieldModifiers(FieldDeclaration f, AST ast, boolean flag) {
		if (flag) {
			f.modifiers().add(ast.newModifier(ModifierKeyword.PUBLIC_KEYWORD));
			f.modifiers().add(ast.newModifier(ModifierKeyword.STATIC_KEYWORD));
		} else {
			f.modifiers().add(ast.newModifier(ModifierKeyword.PUBLIC_KEYWORD));
		}
	}

	/**
	 * add imports of the class
	 * 
	 * @param root
	 * @param id
	 */
	private void addImport(List<ImportDeclaration> root, ImportDeclaration id) {
		int ip = 0;
		// 防止重复import
		if (root != null && root.size() != 0) {
			for (int im1 = 0; im1 < root.size(); im1++) {
				if (!root.get(im1).toString().equals(id.toString())) {
					ip++;
				}
			}
			if (ip == root.size()) {
				root.add(id);
			}
		} else {
			// 可能存在问题
			root.add(id);
		}
	}

	/**
	 * Declaration the field
	 * 
	 * @param types
	 * @param m
	 * @param f
	 * @param cos
	 */
	@SuppressWarnings({ "unchecked", "unused" })
	private void addfieldDeclaration(TypeDeclaration types, MethodDeclaration m, FieldDeclaration f, int cos) {
		int field = 0;
		// 防止字段重复声明
		for (int i = 0; i < types.getMethods().length; i++) {
			if (m.getName() == types.getMethods()[i].getName()) {
				for (int j = 0; j < types.getFields().length; j++) {
					if (!types.getFields()[j].toString().equals(f.toString())) {
						field++;
					}
				}
				if (field == types.getFields().length) {
					types.bodyDeclarations().add(cos, f);
				}
			}
		}
	}

	/**
	 * 
	 * @param ast
	 * @param rwname
	 * @param lock
	 * @return
	 */
	public ExpressionStatement exp(AST ast, String lockname, String rw, String lock) {
		MethodInvocation addreadlock = ast.newMethodInvocation();
		addreadlock.setExpression(ast.newSimpleName(lockname));
		// addreadlock.
		// addreadlock.setExpression(ast.newSimpleName());
		addreadlock.setName(ast.newSimpleName(rw));
		ExpressionStatement ex = ast.newExpressionStatement(addreadlock);

		Expression tmp = ex.getExpression();
		ex.setExpression(ast.newCastExpression());
		MethodInvocation addreadloc = ast.newMethodInvocation();

		addreadloc.setExpression(tmp);
		addreadloc.setName(ast.newSimpleName(lock));
		ExpressionStatement ex1 = ast.newExpressionStatement(addreadloc);
		return ex1;
	}

	public ExpressionStatement exp(AST ast, String rw, String lock) {
		MethodInvocation addreadlock = ast.newMethodInvocation();
		addreadlock.setExpression(ast.newSimpleName("ss"));
		// addreadlock.
		// addreadlock.setExpression(ast.newSimpleName());
		addreadlock.setName(ast.newSimpleName(rw));
		ExpressionStatement ex = ast.newExpressionStatement(addreadlock);

		Expression tmp = ex.getExpression();
		ex.setExpression(ast.newCastExpression());
		MethodInvocation addreadloc = ast.newMethodInvocation();

		addreadloc.setExpression(tmp);
		addreadloc.setName(ast.newSimpleName(lock));
		ExpressionStatement ex1 = ast.newExpressionStatement(addreadloc);
		return ex1;
	}

	@SuppressWarnings("unchecked")
	private void readlockToMethod(AST ast, MethodDeclaration m, String ex, Map<String, String> result) {
		String lockname = result.get(ex);
		ExpressionStatement exstate = exp(ast, lockname, LockSign.READLOCK_SIGN, LockSign.LOCK_SIGN);
		// 释放锁
		ExpressionStatement exstate1 = exp(ast, lockname, LockSign.READLOCK_SIGN, LockSign.UNLOCK_SIGN);
		TryStatement trystate = ast.newTryStatement();
		Block finalblock = ast.newBlock();
		Block tryblock = ast.newBlock();
		Block body = ast.newBlock();
		tryblock = m.getBody();
		m.setBody(null);
		finalblock.statements().add(exstate1);
		trystate.setBody(tryblock);
		trystate.setFinally(finalblock);
		if (body.statements().add(trystate)) {
			body.statements().add(0, exstate);
			m.setBody(body);
		}
	}

	@SuppressWarnings("unchecked")
	private void readlockToBlock(AST ast, MethodDeclaration m, int bl, String ex, Map<String, String> result) {
		String lockname = result.get(ex);
		// 加锁
		ExpressionStatement exstate = exp(ast, lockname, LockSign.READLOCK_SIGN, LockSign.LOCK_SIGN);
		// 释放锁
		ExpressionStatement exstate1 = exp(ast, lockname, LockSign.READLOCK_SIGN, LockSign.UNLOCK_SIGN);
		Block tmp = ast.newBlock();

		tmp = ((SynchronizedStatement) m.getBody().statements().get(bl)).getBody();
		((SynchronizedStatement) m.getBody().statements().get(bl)).setBody(ast.newBlock());
		m.getBody().statements().remove(bl);

		TryStatement trystate = ast.newTryStatement();
		Block finalblock = ast.newBlock();

		finalblock.statements().add(exstate1);
		trystate.setBody(tmp);
		trystate.setFinally(finalblock);
		m.getBody().statements().add(bl, trystate);
		m.getBody().statements().add(bl, exstate);

	}

	@SuppressWarnings("unchecked")
	private void writelockToMethod(AST ast, MethodDeclaration m, String ex, Map<String, String> result) {

		String lockname = result.get(ex);
		ExpressionStatement exstate = exp(ast, lockname, LockSign.WRITELOCK_SIGN, LockSign.LOCK_SIGN);
		// 释放锁
		ExpressionStatement exstate1 = exp(ast, lockname, LockSign.WRITELOCK_SIGN, LockSign.UNLOCK_SIGN);
		TryStatement trystate = ast.newTryStatement();
		Block finalblock = ast.newBlock();
		Block tryblock = ast.newBlock();
		Block body = ast.newBlock();
		tryblock = m.getBody();

		m.setBody(null);
		finalblock.statements().add(exstate1);
		trystate.setBody(tryblock);
		trystate.setFinally(finalblock);
		if (body.statements().add(trystate)) {
			body.statements().add(0, exstate);
			m.setBody(body);
		}
	}

	@SuppressWarnings("unchecked")
	private boolean upgradeToMethod(AST ast, MethodDeclaration m, String ex, Map<String, String> result,
			String linenum) {
		String lockname = result.get(ex);
		ExpressionStatement exstate = exp(ast, lockname, LockSign.WRITELOCK_SIGN, LockSign.LOCK_SIGN);
		// 释放锁
		ExpressionStatement exstate1 = exp(ast, lockname, LockSign.WRITELOCK_SIGN, LockSign.UNLOCK_SIGN);

		ExpressionStatement exstate2 = exp(ast, lockname, LockSign.READLOCK_SIGN, LockSign.LOCK_SIGN);
		// 释放锁
		ExpressionStatement exstate3 = exp(ast, lockname, LockSign.READLOCK_SIGN, LockSign.UNLOCK_SIGN);
		int line = 0;
		int assign = 0;
		char[] c = linenum.toCharArray();
		if (c.length < m.getBody().statements().size()) {
			int tmp = c.length;
			while (tmp < m.getBody().statements().size()) {
				tmp++;
				assign++;
			}
		}
		while (c[line] != 'W') {
			if (c[line] == 'A') {
				assign++;
			}
			line++;
		}

		TryStatement trystate1 = ast.newTryStatement();
		TryStatement trystate2 = ast.newTryStatement();
		Block finalblock1 = ast.newBlock();
		Block finalblock2 = ast.newBlock();
		Block tryblock1 = ast.newBlock();
		Block tryblock2 = ast.newBlock();
		Block body = ast.newBlock();
		Statement st1 = null, st2 = null;
		for (int a = assign; a < line; a++) {
			if (m.getBody().statements().size() == 0) {

			} else {
				st1 = (Statement) m.getBody().statements().get(assign);
				m.getBody().statements().remove(assign);
				tryblock1.statements().add(st1);
			}
		}
		if (assign != 0) {
			for (int t = 0; t < assign; t++) {
				st1 = (Statement) m.getBody().statements().get(0);
				m.getBody().statements().remove(0);
				body.statements().add(t, st1);
			}
		}
		int tm = m.getBody().statements().size();
		if (tm == 0) {
			finalblock1.statements().add(exstate1);
			trystate1.setBody(tryblock1);
			trystate1.setFinally(finalblock1);
			if (body.statements().add(trystate1)) {
				body.statements().add(assign, exstate);
			}
			m.setBody(body);
			return false;
		}

		for (int b = 0; b < tm; b++) {
			st2 = (Statement) m.getBody().statements().get(0);
			m.getBody().statements().remove(0);
			tryblock2.statements().add(st2);

		}

		// m.setBody(null);

		finalblock1.statements().add(exstate3);
		finalblock2.statements().add(exstate1);
		trystate1.setBody(tryblock1);
		trystate1.setFinally(finalblock1);
		trystate2.setBody(tryblock2);
		trystate2.setFinally(finalblock2);
		if (body.statements().add(trystate1)) {
			body.statements().add(assign, exstate2);

		}
		if (body.statements().add(trystate2)) {
			body.statements().add(assign + 2, exstate);
		}
		m.setBody(body);
		return true;
	}

	@SuppressWarnings({ "unused", "unchecked" })
	private void upgradeToMethod2(AST ast, MethodDeclaration m, String linenum) {

		ExpressionStatement read_lock1 = exp(ast, LockSign.READLOCK_SIGN, LockSign.LOCK_SIGN);
		ExpressionStatement read_unlock1 = exp(ast, LockSign.READLOCK_SIGN, LockSign.UNLOCK_SIGN);
		ExpressionStatement write_lock = exp(ast, LockSign.WRITELOCK_SIGN, LockSign.LOCK_SIGN);
		ExpressionStatement write_unlock = exp(ast, LockSign.WRITELOCK_SIGN, LockSign.UNLOCK_SIGN);
		ExpressionStatement read_unlock2 = exp(ast, LockSign.READLOCK_SIGN, LockSign.UNLOCK_SIGN);
		ExpressionStatement read_lock2 = exp(ast, LockSign.READLOCK_SIGN, LockSign.LOCK_SIGN);
		ExpressionStatement get_write = exp(ast, "rwLock", "isWriteLockedByCurrentThread");
		TryStatement trystate = ast.newTryStatement();
		TryStatement trystate1 = ast.newTryStatement();
		Block finalblock = ast.newBlock();
		Block finalblock1 = ast.newBlock();
		Block body = ast.newBlock();

		List<Statement> list = new ArrayList<Statement>();
		for (int j = 0; j < m.getBody().statements().size(); j++) {
			list.add((Statement) m.getBody().statements().get(j));
		}

		List<Statement> tlist = new ArrayList<Statement>();

		Block ifbody = ast.newBlock();
		Block ifbody1 = ast.newBlock();
		Block body1 = ast.newBlock();
		for (int i = 0; i < list.size(); i++) {
			Statement tmps = list.get(i);
			tmps.delete();
			tlist.add(tmps);
			if (list.get(i) instanceof IfStatement) {
				tlist.remove(i);
				IfStatement iftmp = ((IfStatement) list.get(i));
				IfStatement ifstate = ast.newIfStatement();

				// 删除父节点
				iftmp.delete();
				Statement tmp = iftmp.getThenStatement();
				Expression ex = iftmp.getExpression();

				// 把父节点置空
				iftmp.setThenStatement(ast.newAssertStatement());
				iftmp.setExpression(ast.newCastExpression());

				// 内层的try finally
				if (ifbody1.statements().add(tmp)) {

					ifbody1.statements().add(0, read_unlock1);
					ifbody1.statements().add(1, write_lock);
					ifstate.setExpression(ex);
					ifstate.setThenStatement(ifbody1);
					tlist.add(ifstate);
				}
			}

		}
		m.setBody(null);
		for (int i = 0; i < tlist.size(); i++) {
			body1.statements().add(tlist.get(i));
		}
		/**
		 * if(rwLock.isWriteLockedByCurrentThread()) { writelock.unlock(); }else {
		 * readlock.unlock(); }
		 */
		IfStatement ifstate1 = ast.newIfStatement();
		Block ifbody2 = ast.newBlock();
		Block elsebody2 = ast.newBlock();
		Expression ep = get_write.getExpression();
		get_write.setExpression(ast.newCastExpression());

		ifstate1.setExpression(ep);
		ifbody2.statements().add(write_unlock);
		ifstate1.setThenStatement(ifbody2);
		elsebody2.statements().add(read_unlock2);
		ifstate1.setElseStatement(elsebody2);
		// 最外层try finally
		finalblock.statements().add(ifstate1);
		trystate.setBody(body1);
		trystate.setFinally(finalblock);
		if (body.statements().add(trystate)) {
			body.statements().add(0, read_lock1);
			m.setBody(body);
		}

	}

	@SuppressWarnings("unchecked")
	private boolean downgradeToMethod2(AST ast, MethodDeclaration m, String ex, Map<String, String> result,
			String linenum) {

		String lockname = result.get(ex);
		ExpressionStatement exstate = exp(ast, lockname, LockSign.WRITELOCK_SIGN, LockSign.LOCK_SIGN);
		// 释放锁
		ExpressionStatement exstate1 = exp(ast, lockname, LockSign.WRITELOCK_SIGN, LockSign.UNLOCK_SIGN);

		ExpressionStatement exstate2 = exp(ast, lockname, LockSign.READLOCK_SIGN, LockSign.LOCK_SIGN);
		// 释放锁
		ExpressionStatement exstate3 = exp(ast, lockname, LockSign.READLOCK_SIGN, LockSign.UNLOCK_SIGN);
		int line = 0;
		char[] c = linenum.toCharArray();
		while (c[line] != 'R') {
			line++;
		}
		TryStatement trystate1 = ast.newTryStatement();
		TryStatement trystate2 = ast.newTryStatement();
		Block finalblock1 = ast.newBlock();
		Block finalblock2 = ast.newBlock();
		Block tryblock1 = ast.newBlock();
		Block tryblock2 = ast.newBlock();
		Block body = ast.newBlock();
		Statement st1 = null, st2 = null;
		for (int a = 0; a < line; a++) {
			if (m.getBody().statements().size() == 0) {

			} else {
				st1 = (Statement) m.getBody().statements().get(0);
				m.getBody().statements().remove(0);
				tryblock1.statements().add(st1);
			}
		}
		int tm = m.getBody().statements().size();
		// m.setBody(null);
		finalblock1.statements().add(exstate1);
		trystate1.setBody(tryblock1);
		trystate1.setFinally(finalblock1);
		if (body.statements().add(trystate1)) {
			body.statements().add(0, exstate);

		}
		if (tm != 0) {

			for (int b = 0; b < tm; b++) {
				st2 = (Statement) m.getBody().statements().get(0);
				m.getBody().statements().remove(0);
				tryblock2.statements().add(st2);

			}
			finalblock2.statements().add(exstate3);
			trystate2.setBody(tryblock2);
			trystate2.setFinally(finalblock2);
			if (body.statements().add(trystate2)) {
				body.statements().add(2, exstate2);
			}
			m.setBody(body);
			return true;
		}
		m.setBody(body);
		return false;

	}

	@SuppressWarnings("unchecked")
	private void writelockToBlock(AST ast, MethodDeclaration m, int b, String ex, Map<String, String> result) {

		String lock = result.get(ex);

		// 加锁
		ExpressionStatement exstate = exp(ast, lock, LockSign.WRITELOCK_SIGN, LockSign.LOCK_SIGN);
		// 释放锁
		ExpressionStatement exstate1 = exp(ast, lock, LockSign.WRITELOCK_SIGN, LockSign.UNLOCK_SIGN);
		Block tmp = ast.newBlock();

		tmp = ((SynchronizedStatement) m.getBody().statements().get(b)).getBody();
		((SynchronizedStatement) m.getBody().statements().get(b)).setBody(ast.newBlock());
		m.getBody().statements().remove(b);

		TryStatement trystate = ast.newTryStatement();
		Block finalblock = ast.newBlock();

		finalblock.statements().add(exstate1);
		trystate.setBody(tmp);
		trystate.setFinally(finalblock);
		m.getBody().statements().add(b, trystate);
		m.getBody().statements().add(b, exstate);

	}

	/**
	 * 锁降级模式1
	 * 
	 * @param ast
	 * @param m
	 */

	@SuppressWarnings("unchecked")
	private void downgradeToMethod(AST ast, MethodDeclaration m, String lex, Map<String, String> result) {
		String lockname = result.get(lex);
		/**
		 * exstate 读锁加锁 exstate1 读锁释放 exstate3 写锁加锁 exstate4 写锁释放
		 */
		ExpressionStatement read_lock1 = exp(ast, lockname, LockSign.READLOCK_SIGN, LockSign.LOCK_SIGN);
		ExpressionStatement read_unlock1 = exp(ast, lockname, LockSign.READLOCK_SIGN, LockSign.UNLOCK_SIGN);
		ExpressionStatement write_lock = exp(ast, lockname, LockSign.WRITELOCK_SIGN, LockSign.LOCK_SIGN);
		ExpressionStatement write_unlock = exp(ast, lockname, LockSign.WRITELOCK_SIGN, LockSign.UNLOCK_SIGN);
		ExpressionStatement read_unlock2 = exp(ast, lockname, LockSign.READLOCK_SIGN, LockSign.UNLOCK_SIGN);
		ExpressionStatement read_lock2 = exp(ast, lockname, LockSign.READLOCK_SIGN, LockSign.LOCK_SIGN);

		TryStatement trystate = ast.newTryStatement();
		TryStatement trystate1 = ast.newTryStatement();
		Block finalblock = ast.newBlock();
		Block finalblock1 = ast.newBlock();
		Block body = ast.newBlock();

		List<Statement> list = new ArrayList<Statement>();
		for (int j = 0; j < m.getBody().statements().size(); j++) {
			list.add((Statement) m.getBody().statements().get(j));
		}

		List<Statement> tlist = new ArrayList<Statement>();

		Block ifbody = ast.newBlock();
		Block ifbody1 = ast.newBlock();
		Block ifbody2 = ast.newBlock();
		Block body1 = ast.newBlock();
		for (int i = 0; i < list.size(); i++) {
			Statement tmps = list.get(i);
			tmps.delete();
			tlist.add(tmps);
			if (tmps instanceof IfStatement) {
				tlist.remove(i);
				IfStatement iftmp = ((IfStatement) list.get(i));
				IfStatement ifstate = ast.newIfStatement();
				IfStatement ifstate2 = ast.newIfStatement();
				// 删除父节点
				iftmp.delete();
				Statement tmp = iftmp.getThenStatement();
				Expression ex = iftmp.getExpression();
				// Name n=ast.newName("flag");
				// Expression ex3=;
				ex.getParent().delete();

				// 把父节点置空
				iftmp.setThenStatement(ast.newAssertStatement());

				iftmp.setExpression(ast.newCastExpression());
				// ExpressionStatement ex1 = ast.newExpressionStatement(ex);
				// ExpressionStatement
				// ex3=ast.newExpressionStatement(ast.newSimpleName(s.toString()));
				// 内层的try finally
				if (ifbody.statements().add(tmp)) {

					finalblock1.statements().add(read_lock2);
					finalblock1.statements().add(write_unlock);
					trystate1.setBody(ifbody);
					trystate1.setFinally(finalblock1);
					ifbody1.statements().add(trystate1);
					ASTNode ifCondition = rewrite.createStringPlaceholder(ex.toString(),
							ASTNode.CONDITIONAL_EXPRESSION);
					ifstate.setExpression((Expression) ifCondition);
					// ExpressionStatement ex1 = ast.newExpressionStatement(ex);
					ifstate.setThenStatement(ifbody1);
					// ifbody1.statements().add(0, read_unlock1);
					// ifbody1.statements().add(1, write_lock);
					ifbody2.statements().add(ifstate);
					ifbody2.statements().add(0, read_unlock1);
					ifbody2.statements().add(1, write_lock);

					ifstate2.setExpression(ex);
					ifstate2.setThenStatement(ifbody2);
					tlist.add(ifstate2);
				}
			}

		}
		m.setBody(null);
		for (int i = 1; i < tlist.size(); i++) {
//			if(tlist.get(i) instanceof IfStatement) {
//				System.out.println("i="+i);
//				System.out.println("s"+tlist.size()); 
//				for(int t=i+1;t<tlist.size();t++) {
			body1.statements().add(tlist.get(i));
			// }
			// }

		}

		// 最外层try finally
		finalblock.statements().add(read_unlock2);
		trystate.setBody(body1);
		trystate.setFinally(finalblock);
		if (body.statements().add(trystate)) {
			body.statements().add(0, read_lock1);
			m.setBody(body);
			m.getBody().statements().add(1, tlist.get(0));
		}

	}

	@SuppressWarnings("unchecked")
	private void downgradeToBlock(AST ast, MethodDeclaration m, int bl, String lex, Map<String, String> result) {

		String lockname = result.get(lex);

		ExpressionStatement read_lock1 = exp(ast, lockname, LockSign.READLOCK_SIGN, LockSign.LOCK_SIGN);
		ExpressionStatement read_unlock1 = exp(ast, lockname, LockSign.READLOCK_SIGN, LockSign.UNLOCK_SIGN);
		ExpressionStatement write_lock = exp(ast, lockname, LockSign.WRITELOCK_SIGN, LockSign.LOCK_SIGN);
		ExpressionStatement write_unlock = exp(ast, lockname, LockSign.WRITELOCK_SIGN, LockSign.UNLOCK_SIGN);
		ExpressionStatement read_unlock2 = exp(ast, lockname, LockSign.READLOCK_SIGN, LockSign.UNLOCK_SIGN);
		ExpressionStatement read_lock2 = exp(ast, lockname, LockSign.READLOCK_SIGN, LockSign.LOCK_SIGN);

		Block tmp = ast.newBlock();

		tmp = ((SynchronizedStatement) m.getBody().statements().get(bl)).getBody();
		((SynchronizedStatement) m.getBody().statements().get(bl)).setBody(ast.newBlock());
		m.getBody().statements().remove(bl);

		TryStatement trystate = ast.newTryStatement();
		TryStatement trystate1 = ast.newTryStatement();
		Block finalblock = ast.newBlock();
		Block finalblock1 = ast.newBlock();
		Block body = ast.newBlock();

		List<Statement> list = new ArrayList<Statement>();
		for (int j = 0; j < tmp.statements().size(); j++) {
			list.add((Statement) tmp.statements().get(j));
		}

		List<Statement> tlist = new ArrayList<Statement>();

		Block ifbody = ast.newBlock();
		Block ifbody1 = ast.newBlock();
		Block body1 = ast.newBlock();
		for (int i = 0; i < list.size(); i++) {
			Statement tmps = list.get(i);
			tmps.delete();
			tlist.add(tmps);
			if (list.get(i) instanceof IfStatement) {
				tlist.remove(i);
				IfStatement iftmp = ((IfStatement) list.get(i));
				IfStatement ifstate = ast.newIfStatement();

				// 删除父节点
				iftmp.delete();
				Statement tmp1 = iftmp.getThenStatement();
				Expression ex = iftmp.getExpression();

				// 把父节点置空
				iftmp.setThenStatement(ast.newAssertStatement());
				iftmp.setExpression(ast.newCastExpression());

				// 内层的try finally
				if (ifbody.statements().add(tmp1)) {

					finalblock1.statements().add(read_lock2);
					finalblock1.statements().add(write_unlock);
					trystate1.setBody(ifbody);
					trystate1.setFinally(finalblock1);
					ifbody1.statements().add(trystate1);
					ifbody1.statements().add(0, read_unlock1);
					ifbody1.statements().add(1, write_lock);
					ifstate.setExpression(ex);
					ifstate.setThenStatement(ifbody1);
					tlist.add(ifstate);
				}
			}

		}
		m.setBody(null);

		for (int i = 0; i < tlist.size(); i++) {
			body1.statements().add(tlist.get(i));
		}

		// 最外层try finally
		finalblock.statements().add(read_unlock2);
		trystate.setBody(body1);
		trystate.setFinally(finalblock);
		if (body.statements().add(trystate)) {
			body.statements().add(0, read_lock1);
			m.setBody(body);
		}

	}

	private void downgradeToBlock2(AST ast, MethodDeclaration m, int bl, String ex, Map<String, String> result,
			String linenum) {

		String lockname = result.get(ex);
		ExpressionStatement exstate = exp(ast, lockname, LockSign.WRITELOCK_SIGN, LockSign.LOCK_SIGN);
		// 释放锁
		ExpressionStatement exstate1 = exp(ast, lockname, LockSign.WRITELOCK_SIGN, LockSign.UNLOCK_SIGN);

		ExpressionStatement exstate2 = exp(ast, lockname, LockSign.READLOCK_SIGN, LockSign.LOCK_SIGN);
		// 释放锁
		ExpressionStatement exstate3 = exp(ast, lockname, LockSign.READLOCK_SIGN, LockSign.UNLOCK_SIGN);

		Block tmp = ast.newBlock();

		tmp = ((SynchronizedStatement) m.getBody().statements().get(bl)).getBody();
		((SynchronizedStatement) m.getBody().statements().get(bl)).setBody(ast.newBlock());
		m.getBody().statements().remove(bl);

		int line = 0;
		int cos = 0;
		char[] c = linenum.toCharArray();
		while (c[cos] != 'R') {
			if (c[cos] == 'M') {
				line--;
			}
			line++;
			cos++;
		}
		toBlock(ast, m, line, bl, tmp, exstate2, exstate3, exstate, exstate1);
	}

	private void upgradeToBlock(AST ast, MethodDeclaration m, int bl, String ex, Map<String, String> result,
			String linenum) {
		String lockname = result.get(ex);

		ExpressionStatement exstate = exp(ast, lockname, LockSign.WRITELOCK_SIGN, LockSign.LOCK_SIGN);
		// 释放锁
		ExpressionStatement exstate1 = exp(ast, lockname, LockSign.WRITELOCK_SIGN, LockSign.UNLOCK_SIGN);

		ExpressionStatement exstate2 = exp(ast, lockname, LockSign.READLOCK_SIGN, LockSign.LOCK_SIGN);
		// 释放锁
		ExpressionStatement exstate3 = exp(ast, lockname, LockSign.READLOCK_SIGN, LockSign.UNLOCK_SIGN);

		Block tmp = ast.newBlock();

		tmp = ((SynchronizedStatement) m.getBody().statements().get(bl)).getBody();
		((SynchronizedStatement) m.getBody().statements().get(bl)).setBody(ast.newBlock());
		m.getBody().statements().remove(bl);

		int line = 0;
		int cos = 0;
		char[] c = linenum.toCharArray();
		while (c[cos] != 'W') {
			if (c[cos] == 'M') {
				line--;
			}
			line++;
			cos++;
		}
		toBlock(ast, m, line, bl, tmp, exstate, exstate1, exstate2, exstate3);
	}

	@SuppressWarnings("unchecked")
	private void toBlock(AST ast, MethodDeclaration m, int line, int bl, Block tmp, ExpressionStatement exstate,
			ExpressionStatement exstate1, ExpressionStatement exstate2, ExpressionStatement exstate3) {
		TryStatement trystate1 = ast.newTryStatement();
		TryStatement trystate2 = ast.newTryStatement();
		Block finalblock1 = ast.newBlock();
		Block finalblock2 = ast.newBlock();
		Block tryblock1 = ast.newBlock();
		Block tryblock2 = ast.newBlock();
		// Block body = ast.newBlock();
		Statement st1 = null, st2 = null;
		for (int a = 0; a < line; a++) {
			st1 = (Statement) tmp.statements().get(0);
			tmp.statements().remove(0);
			tryblock1.statements().add(st1);
		}
		int tm = tmp.statements().size();
		for (int b = 0; b < tm; b++) {
			st2 = (Statement) tmp.statements().get(0);
			tmp.statements().remove(0);
			tryblock2.statements().add(st2);

		}
		finalblock1.statements().add(exstate3);
		finalblock2.statements().add(exstate1);
		trystate1.setBody(tryblock1);
		trystate1.setFinally(finalblock1);
		trystate2.setBody(tryblock2);
		trystate2.setFinally(finalblock2);
		// m.setBody(null);
		m.getBody().statements().add(bl, trystate1);
		m.getBody().statements().add(bl, exstate2);
		m.getBody().statements().add(bl + 2, trystate2);
		m.getBody().statements().add(bl + 2, exstate);
	}

	private void decalock(AST ast, TypeDeclaration types, LockSet ls, List<String> list, Map<String, String> result) {
		ls.stLock();
		ls.inLock();
		Map<IField, String> slockmap = ls.getsmap();
		Map<IField, String> lockmap = ls.getmap();
		if (ls.staticmap().size() != 0 && !list.contains("stlock")) {
			addlock(ast, types, ls.staticmap().get("static"), true);
			result.put("static", ls.staticmap().get("static"));
			list.add(ls.staticmap().get("static"));
		}
		if (ls.thismap().size() != 0 && !list.contains("tlock")) {
			addlock(ast, types, ls.thismap().get("this"), false);
			result.put("this", ls.thismap().get("this"));
			list.add(ls.thismap().get("this"));
		}
		for (IField key : slockmap.keySet()) {
			if (!list.contains(slockmap.get(key))) {
				addlock(ast, types, slockmap.get(key), true);
				list.add(slockmap.get(key));
			}
			result.put(key.getName().toString(), slockmap.get(key));
		}
		for (IField key : lockmap.keySet()) {
			if (!list.contains(lockmap.get(key))) {
				addlock(ast, types, lockmap.get(key), false);
				list.add(lockmap.get(key));
			}
			result.put(key.getName().toString(), lockmap.get(key));
		}
	}

	@SuppressWarnings("unchecked")
	private void addlock(AST ast, TypeDeclaration types, String lockname, boolean flag) {
		VariableDeclarationFragment lock = ast.newVariableDeclarationFragment();
		lock.setName(ast.newSimpleName(lockname));
		ClassInstanceCreation creation = ast.newClassInstanceCreation();
		creation.setType(ast.newSimpleType(ast.newSimpleName("ReentrantReadWriteLock")));
		lock.setInitializer(creation);

		FieldDeclaration fieldDeclaration = ast.newFieldDeclaration(lock);
		fieldDeclaration.setType(ast.newSimpleType(ast.newName("ReentrantReadWriteLock")));
		aadfieldModifiers(fieldDeclaration, ast, flag);

		types.bodyDeclarations().add(0, fieldDeclaration);
	}
	
	
	/**
	  *   获取当前类下所有锁句柄
	 * @param lockex 用来存储锁句柄
	 * @param inmap  用来存储方法类型的映射关系
	 * @param types  当前类
	 */
	private void getlockex(List<String> lockex, Map<MethodDeclaration, String> inmap, TypeDeclaration types) {
		MethodDeclaration[] ms = types.getMethods();
		// 遍历当前类的所有方法
		for (MethodDeclaration m : ms) {
			// 遍历修饰符
			for (int i = 0; i < m.modifiers().size(); i++) {
				// 获取同步方法的锁
				if (m.modifiers().get(i).toString().equals("synchronized")) {
					// 判断是否是静态方法，static和synchronized的前后位置
						for (int j = 0; j < m.modifiers().size(); j++) {
							if (m.modifiers().get(j).toString().equals("static")) {
								lockex.add("static");
								inmap.put(m, "static");
								break;
							} else if (j + 1 == m.modifiers().size()) {
								lockex.add("this");
								inmap.put(m, "this");
							}
						}

				} else if (i + 1 == m.modifiers().size()) {
					// 获取同步块的锁
					for (int b = 0; b < m.getBody().statements().size(); b++) {
						if (m.getBody().statements().get(b) instanceof SynchronizedStatement) {
							//获取锁句柄
							lockex.add(((SynchronizedStatement) m.getBody().statements().get(b)).getExpression()
									.toString());
						}
					}
				}
			}

		}
	}

	private void print_num() {
		System.out.println("同步方法:" + sy_num + "  " + "锁升级:" + sy_up_num + "  " + "锁降级:" + sy_down_num + "  " + "读锁:"
				+ sy_read_num + "  " + "写锁:" + sy_write_num);
		System.out.println("同步块:" + bl_num + "  " + "锁升级:" + bl_up_num + "  " + "锁降级:" + bl_down_num + "  " + "读锁:"
				+ bl_read_num + "  " + "写锁:" + bl_write_num);
	}

	public void setsynMethod(boolean n) {
		synMethod = n;
	}

	public void setsynBlock(boolean n) {
		synBlock = n;
	}

}
