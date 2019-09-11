package rwlockrefactoring.refactoring;

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Label;


/**
 * WizardPage
 * @author Shao
 *
 */
public class RWLockRefactoringWizardPage extends UserInputWizardPage{
	
	//重构同步方法的按钮
	Button btnCheck1;
	//重构同步块的按钮
	Button btnCheck2;
	Label labName;
	Text txtTimeOut;
	
	public RWLockRefactoringWizardPage(String name) {
		super(name);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void createControl(Composite parent) {
		//定义UI
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout lay = new GridLayout();
		lay.numColumns = 2;
		composite.setLayout(lay);
		
		//按钮实现
		btnCheck1 = new Button(composite, SWT.CHECK);
		btnCheck1.setText("refactor synchronized method");
		btnCheck2 = new Button(composite, SWT.CHECK);
		btnCheck2.setText("refactor synchronized block");
		
		GridData gdBtnCheck = new GridData();
		gdBtnCheck.horizontalSpan = 2;
		gdBtnCheck.horizontalAlignment = GridData.FILL;
		btnCheck1.setLayoutData(gdBtnCheck);
		btnCheck2.setLayoutData(gdBtnCheck);
		
//		labName = new Label(composite, SWT.WRAP);
//		labName.setText("TimeOut:");
//		GridData gdLabName = new GridData();
//		gdLabName.horizontalAlignment = GridData.BEGINNING;
//		gdLabName.grabExcessHorizontalSpace = true;
//		labName.setLayoutData(gdLabName);
//		txtTimeOut = new Text(composite, SWT.SINGLE | SWT.BORDER);
//		GridData gdTxtTimeOut = new GridData();
//		gdTxtTimeOut.horizontalAlignment = GridData.END;
//		gdLabName.grabExcessHorizontalSpace = true;
//		txtTimeOut.setLayoutData(gdTxtTimeOut);
//		txtTimeOut.setText("500");
		// init status
		//labName.setEnabled(false);
		//txtTimeOut.setEnabled(false);
		// add listener
		defineListener();
		// 将 composite 纳入框架的控制
		setControl(composite);
		Dialog.applyDialogFont(composite);
//		notifyStatus(true, "refactoring finished");
	}
	private void notifyStatus(boolean valid, String message) { 
		 // 设置错误信息
		 setErrorMessage(message); 
		 // 设置页面完成状态
		 setPageComplete(valid); 
	 }
	/**
	 * define the action listener
	 */
	private void defineListener(){
		RWLockRefactoring refactoring = (RWLockRefactoring) getRefactoring();
		
		
		btnCheck1.addSelectionListener(new SelectionListener(){

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				btnCheck1.setEnabled(true);
			}

			@Override
			public void widgetSelected(SelectionEvent se) {
				if(btnCheck1.getEnabled()){
					System.out.println(btnCheck1.getEnabled());
					refactoring.synMethod = true;
					//txtTimeOut.setEnabled(true);
				}else{
					refactoring.synMethod = false;
					//txtTimeOut.setEnabled(false);
				}
				
			}
			
		});
		
		btnCheck2.addSelectionListener(new SelectionListener(){

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				btnCheck2.setEnabled(false);
			}

			@Override
			public void widgetSelected(SelectionEvent se) {
				if(btnCheck2.getEnabled()){
					System.out.println(btnCheck2.getEnabled());
					refactoring.synBlock = true;
					//txtTimeOut.setEnabled(true);
				}else{
					refactoring.synBlock = false;
					//txtTimeOut.setEnabled(false);
				}
				
			}
			
		});

	
	}	
		

		
	}


