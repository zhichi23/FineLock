package rwlockrefactoring.refactoring;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;

public interface LockRefactoring {
	// lock downgrading 1
	public boolean refactoring_down(AST ast, MethodDeclaration m, String lex);

	// lock downgrading 2
	public boolean refactoring_downs(AST ast, MethodDeclaration m, String ex, String linenum);

	// lock splitting 1
	public boolean refactoring_up(AST ast, MethodDeclaration m, String ex);

	// lock splitting 2
	public boolean refactoring_ups(AST ast, MethodDeclaration m, String ex, String linenum);

	// read lock
	public boolean refactoring_read(AST ast, MethodDeclaration m, String ex);

	// write lock
	public boolean refactoring_write(AST ast, MethodDeclaration m, String ex);

	// null
	public void refactoring_null(AST ast, MethodDeclaration m);

	// add lock
	public ExpressionStatement exp(AST ast, String lockname, String rw, String lock);
}