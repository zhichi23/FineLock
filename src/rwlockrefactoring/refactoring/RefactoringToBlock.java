package rwlockrefactoring.refactoring;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SynchronizedStatement;
import org.eclipse.jdt.core.dom.TryStatement;

import rwlockrefactoring.util.LockSign;

public class RefactoringToBlock implements LockRefactoring {

	Map<String, String> result;
	int bl;

	RefactoringToBlock(Map<String, String> result) {
		this.result = result;
	}

	RefactoringToBlock(Map<String, String> result, int bl) {
		this.result = result;
		this.bl = bl;
	}

	@Override
	public boolean refactoring_down(AST ast, MethodDeclaration m, String lex) {
		// TODO Auto-generated method stub
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

				iftmp.delete();
				Statement tmp1 = iftmp.getThenStatement();
				Expression ex = iftmp.getExpression();

				iftmp.setThenStatement(ast.newAssertStatement());
				iftmp.setExpression(ast.newCastExpression());

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

		finalblock.statements().add(read_unlock2);
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
	public boolean refactoring_downs(AST ast, MethodDeclaration m, String ex, String linenum) {
		// TODO Auto-generated method stub
		String lockname = result.get(ex);
		ExpressionStatement exstate = exp(ast, lockname, LockSign.WRITELOCK_SIGN, LockSign.LOCK_SIGN);
		ExpressionStatement exstate1 = exp(ast, lockname, LockSign.WRITELOCK_SIGN, LockSign.UNLOCK_SIGN);

		ExpressionStatement exstate2 = exp(ast, lockname, LockSign.READLOCK_SIGN, LockSign.LOCK_SIGN);
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
		toBlock(ast, m, line, tmp, exstate2, exstate3, exstate, exstate1);
		return true;
	}

	@Override
	public boolean refactoring_up(AST ast, MethodDeclaration m, String ex) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean refactoring_ups(AST ast, MethodDeclaration m, String ex, String linenum) {
		// TODO Auto-generated method stub
		String lockname = result.get(ex);

		ExpressionStatement exstate = exp(ast, lockname, LockSign.WRITELOCK_SIGN, LockSign.LOCK_SIGN);
		ExpressionStatement exstate1 = exp(ast, lockname, LockSign.WRITELOCK_SIGN, LockSign.UNLOCK_SIGN);

		ExpressionStatement exstate2 = exp(ast, lockname, LockSign.READLOCK_SIGN, LockSign.LOCK_SIGN);
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
		toBlock(ast, m, line, tmp, exstate, exstate1, exstate2, exstate3);
		return false;
	}

	@Override
	public boolean refactoring_read(AST ast, MethodDeclaration m, String ex) {
		// TODO Auto-generated method stub
		String lockname = result.get(ex);
		ExpressionStatement exstate = exp(ast, lockname, LockSign.READLOCK_SIGN, LockSign.LOCK_SIGN);
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
		return false;
	}

	@Override
	public boolean refactoring_write(AST ast, MethodDeclaration m, String ex) {
		// TODO Auto-generated method stub
		String lockname = result.get(ex);
		ExpressionStatement exstate = exp(ast, lockname, LockSign.READLOCK_SIGN, LockSign.LOCK_SIGN);
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

		return true;
	}

	@Override
	public ExpressionStatement exp(AST ast, String lockname, String rw, String lock) {
		// TODO Auto-generated method stub
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

	private void toBlock(AST ast, MethodDeclaration m, int line, Block tmp, ExpressionStatement exstate,
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

	@Override
	public void refactoring_null(AST ast, MethodDeclaration m) {
		// TODO Auto-generated method stub
		ExpressionStatement exstate = exp(ast, "nblock", LockSign.WRITELOCK_SIGN, LockSign.LOCK_SIGN);
		ExpressionStatement exstate1 = exp(ast, "nblock", LockSign.WRITELOCK_SIGN, LockSign.UNLOCK_SIGN);
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

}
