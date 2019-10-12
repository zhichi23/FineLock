package rwlockrefactoring.refactoring;

import java.util.List;
import java.util.Map;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SynchronizedStatement;
import org.eclipse.jdt.core.dom.Modifier.ModifierKeyword;

import com.ibm.wala.classLoader.IField;

import rwlockrefactoring.analysis.LockSet;

import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

public class RefactoringUtil {
	
	
	
	//import������
	public void addImport(List<ImportDeclaration> root, ImportDeclaration id) {
		int ip = 0;
		// ��ֹ�ظ�import
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
			// ���ܴ�������
			root.add(id);
		}
	}
	
	
	//���η��Ķ���
	public void aadfieldModifiers(FieldDeclaration f, AST ast, boolean static_flag) {
		//��̬���η���־
		if (static_flag) {
			f.modifiers().add(ast.newModifier(ModifierKeyword.PUBLIC_KEYWORD));
			f.modifiers().add(ast.newModifier(ModifierKeyword.STATIC_KEYWORD));
		} else {
			f.modifiers().add(ast.newModifier(ModifierKeyword.PUBLIC_KEYWORD));
		}
	}	
	
	/**
	 * �����ʽ���Ӧ����ӳ���ϵ�ļ���
	 * @param ast
	 * @param types
	 * @param ls
	 * @param list
	 * @param result ���ʽ������ӳ���ϵ
	 */
	 public void decalock(AST ast, TypeDeclaration types, LockSet ls, List<String> list, 
			Map<String, String> result) {
		ls.makelock();
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
	public void addlock(AST ast, TypeDeclaration types, String lockname, boolean flag) {
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
	 * ��ȡ��ǰ�������������
	 * 
	 * @param lockex �����洢�����
	 * @param inmap  �����洢�������͵�ӳ���ϵ
	 * @param types  ��ǰ��
	 */
	public void getlockex(List<String> lockex, Map<MethodDeclaration, String> inmap, 
			TypeDeclaration types,List<MethodDeclaration> abs) {
		if(!types.isInterface()) {
		MethodDeclaration[] ms = types.getMethods();
		// ������ǰ������з���
		for (MethodDeclaration m : ms) {
			// �������η�
			for (int i = 0; i < m.modifiers().size(); i++) {
				// ��ȡͬ����������
				if (m.modifiers().get(i).toString().equals("synchronized")) {
					// �ж��Ƿ��Ǿ�̬������static��synchronized��ǰ��λ��
					for (int j = 0; j < m.modifiers().size(); j++) {
						if (m.modifiers().get(j).toString().equals("static")) {
							lockex.add("static");
							inmap.put(m, "static");
							break;
						} else if (m.modifiers().get(j).toString().equals("abstract")||
								m.modifiers().get(j).toString().equals("native")) {
							abs.add(m);
						} else if (j + 1 == m.modifiers().size()) {
							lockex.add("this");
							inmap.put(m, "this");
						}
					}
				} else if (m.modifiers().get(i).toString().equals("abstract")||
						m.modifiers().get(i).toString().equals("native")) {
					abs.add(m);
				} else if (i + 1 == m.modifiers().size()) {
					// ��ȡͬ�������
					//System.out.println(m.getName() + "----" + types.getName().toString());
					if(m.getBody()!=null) {
					for (int b = 0; b < m.getBody().statements().size(); b++) {
						if (m.getBody().statements().get(b) instanceof SynchronizedStatement) {
							// ��ȡ�����
							lockex.add(((SynchronizedStatement) m.getBody().statements().get(b)).getExpression()
									.toString());
						}
					}
				}}
			}

		}}
	}
	
	
	
	//�ֶε�����
	@Deprecated
	private void addfieldDeclaration(TypeDeclaration types, MethodDeclaration m, FieldDeclaration f, int cos) {
		int field = 0;
		// ��ֹ�ֶ��ظ�����
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

	


}
