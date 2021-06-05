package rwlockrefactoring.actions;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ltk.ui.refactoring.RefactoringWizardOpenOperation;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import rwlockrefactoring.refactoring.RWLockRefactoring;
import rwlockrefactoring.refactoring.RWLockRefactoringWizard;

/**
 * 
 * @author Shuai
 * @version 2.0
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
	public void selectionChanged(IAction action, ISelection selection) {
		if (selection.isEmpty())
			select = null;
		else if (selection instanceof IStructuredSelection) {
			IStructuredSelection strut = ((IStructuredSelection) selection);
			if (strut.size() != 1)
				select = null;
			if (strut.getFirstElement() instanceof IPackageFragment) {
				select = (IPackageFragment) strut.getFirstElement();
			} else if (strut.getFirstElement() instanceof ICompilationUnit) {
				select = (ICompilationUnit) strut.getFirstElement();
			} else if (strut.getFirstElement() instanceof IJavaElement)
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
