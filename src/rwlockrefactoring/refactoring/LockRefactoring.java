package rwlockrefactoring.refactoring;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;

public interface LockRefactoring {
	//锁降级模式１
	public boolean refactoring_down(AST ast, MethodDeclaration m, String lex);
	//锁降级模式２　分解
	public boolean refactoring_downs(AST ast, MethodDeclaration m, String ex,String linenum);
	//锁升级模式１
	public boolean refactoring_up(AST ast, MethodDeclaration m, String ex);
	//锁升级模式２ 分解
	public boolean refactoring_ups(AST ast, MethodDeclaration m, String ex,String linenum);
	//加读锁
	public boolean refactoring_read(AST ast, MethodDeclaration m, String ex);
	//加写锁
	public boolean refactoring_write(AST ast, MethodDeclaration m, String ex);
	
	public void refactoring_null(AST ast, MethodDeclaration m);

	//加锁
	public ExpressionStatement exp(AST ast, String lockname, String rw, String lock);
}