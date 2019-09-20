package rwlockrefactoring.refactoring;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.TryStatement;

import rwlockrefactoring.util.LockSign;

public class RefactoringToMethod implements LockRefactoring{
	
	Map<String, String> result;
	RefactoringToMethod(Map<String, String> result){
		this.result=result;
	}

	@Override
	public boolean refactoring_down(AST ast, MethodDeclaration m, String lex) {
		// TODO Auto-generated method stub
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
					//TODO 待解决
					//ASTNode ifCondition = rewrite.createStringPlaceholder(ex.toString(),
					//		ASTNode.CONDITIONAL_EXPRESSION);
					//ifstate.setExpression((Expression) ifCondition);
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
		return false;
	}

	@Override
	public boolean refactoring_downs(AST ast, MethodDeclaration m, String ex,String linenum) {
		// TODO Auto-generated method stub
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

	@Override
	public boolean refactoring_up(AST ast, MethodDeclaration m, String ex) {
		// TODO Auto-generated method stub
		String lockname = result.get(ex);
		ExpressionStatement read_lock1 = exp(ast, lockname,LockSign.READLOCK_SIGN, LockSign.LOCK_SIGN);
		ExpressionStatement read_unlock1 = exp(ast, lockname,LockSign.READLOCK_SIGN, LockSign.UNLOCK_SIGN);
		ExpressionStatement write_lock = exp(ast, lockname,LockSign.WRITELOCK_SIGN, LockSign.LOCK_SIGN);
		ExpressionStatement write_unlock = exp(ast, lockname,LockSign.WRITELOCK_SIGN, LockSign.UNLOCK_SIGN);
		ExpressionStatement read_unlock2 = exp(ast,lockname,LockSign.READLOCK_SIGN, LockSign.UNLOCK_SIGN);
		ExpressionStatement read_lock2 = exp(ast, lockname,LockSign.READLOCK_SIGN, LockSign.LOCK_SIGN);
		ExpressionStatement get_write = exp(ast, lockname,"rwLock", "isWriteLockedByCurrentThread");
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
				Expression ex1 = iftmp.getExpression();

				// 把父节点置空
				iftmp.setThenStatement(ast.newAssertStatement());
				iftmp.setExpression(ast.newCastExpression());

				// 内层的try finally
				if (ifbody1.statements().add(tmp)) {

					ifbody1.statements().add(0, read_unlock1);
					ifbody1.statements().add(1, write_lock);
					ifstate.setExpression(ex1);
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
			return true;
		}
		return false;
	}

	@Override
	public boolean refactoring_ups(AST ast, MethodDeclaration m, String ex,String linenum) {
		// TODO Auto-generated method stub
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
		//尝试把变量的声明放在锁外面
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

	@Override
	public boolean refactoring_read(AST ast, MethodDeclaration m, String ex) {
		// TODO Auto-generated method stub
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
			return true;
		}
		return false;
	}

	@Override
	public boolean refactoring_write(AST ast, MethodDeclaration m, String ex) {
		// TODO Auto-generated method stub
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
			return true;
		}
		return false;
	}

	
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

	@Override
	public void refactoring_null(AST ast, MethodDeclaration m) {
		// TODO Auto-generated method stub
		ExpressionStatement exstate = exp(ast, "nulock", LockSign.WRITELOCK_SIGN, LockSign.LOCK_SIGN);
		// 释放锁
		ExpressionStatement exstate1 = exp(ast, "nulock", LockSign.WRITELOCK_SIGN, LockSign.UNLOCK_SIGN);
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
}
