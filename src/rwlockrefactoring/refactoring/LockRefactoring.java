package rwlockrefactoring.refactoring;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;

public interface LockRefactoring {
	//������ģʽ��
	public boolean refactoring_down(AST ast, MethodDeclaration m, String lex);
	//������ģʽ�����ֽ�
	public boolean refactoring_downs(AST ast, MethodDeclaration m, String ex,String linenum);
	//������ģʽ��
	public boolean refactoring_up(AST ast, MethodDeclaration m, String ex);
	//������ģʽ�� �ֽ�
	public boolean refactoring_ups(AST ast, MethodDeclaration m, String ex,String linenum);
	//�Ӷ���
	public boolean refactoring_read(AST ast, MethodDeclaration m, String ex);
	//��д��
	public boolean refactoring_write(AST ast, MethodDeclaration m, String ex);
	
	public void refactoring_null(AST ast, MethodDeclaration m);

	//����
	public ExpressionStatement exp(AST ast, String lockname, String rw, String lock);
}