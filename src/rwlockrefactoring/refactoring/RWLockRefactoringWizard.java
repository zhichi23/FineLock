package rwlockrefactoring.refactoring;

import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;
import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;

public class RWLockRefactoringWizard extends RefactoringWizard {
	UserInputWizardPage page;

	public RWLockRefactoringWizard(Refactoring refactoring) {
		super(refactoring, WIZARD_BASED_USER_INTERFACE);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected void addUserInputPages() {
		// TODO Auto-generated method stub
		page = new RWLockRefactoringWizardPage("refactor annotation");
		addPage(page);
	}

}
