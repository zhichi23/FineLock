package rwlockrefactoring.actions;

import java.io.IOException;

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ltk.ui.refactoring.RefactoringWizardOpenOperation;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import com.ibm.wala.ipa.callgraph.CallGraphBuilderCancelException;
import com.ibm.wala.ipa.cha.ClassHierarchyException;

import rwlockrefactoring.refactoring.RWLockRefactoring;
import rwlockrefactoring.refactoring.RWLockRefactoringWizard;

/**
 * 
 * 
 * 
 * @author Shao
 * @version 1.0
 */
public class RefactorManageAction implements IWorkbenchWindowActionDelegate {
	// used by method selectionChanged
	IJavaElement select;
	// open the window
	IWorkbenchWindow window;

	/**
	 * This method is used to open a window
	 */
	@Override
	public void run(IAction action) {
		Shell shell = window.getShell();
		RWLockRefactoring refactor = new RWLockRefactoring(select);
		RWLockRefactoringWizard wizard = new RWLockRefactoringWizard(refactor);
		RefactoringWizardOpenOperation op = new RefactoringWizardOpenOperation(wizard);

		try {
			op.run(shell, "Inserting @Override Annotation");
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * changed as user selected
	 */
	@Override
	// selection中记录了用户选择部分。首先判断选择的部分数目是否唯一，然后判断这个唯一的选择部分是不是Java模型元素
	public void selectionChanged(IAction action, ISelection selection) {
		if (selection.isEmpty())
			select = null;
		else if (selection instanceof IStructuredSelection) {	
			IStructuredSelection strut = ((IStructuredSelection) selection);
			if (strut.size() != 1)
				select = null;
			if(strut.getFirstElement() instanceof IPackageFragment) {
				select = (IPackageFragment) strut.getFirstElement();
			}else
			if(strut.getFirstElement() instanceof ICompilationUnit) {
				select = (ICompilationUnit) strut.getFirstElement();
			}else
			if (strut.getFirstElement() instanceof IJavaElement)
				select = (IJavaElement) strut.getFirstElement();
		} else
			select = null;
//		action.setEnabled(true);
		action.setEnabled(select != null);
	}

	@Override
	public void dispose() {

	}

	@Override
	public void init(IWorkbenchWindow window) {
		this.window = window;
	}

}
