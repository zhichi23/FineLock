package rwlockrefactoring.nfa;

import java.util.regex.Pattern;

public interface RefactoringString {
	Pattern m_p1=Pattern.compile("R*CWTR+");
	//������
	Pattern m_p2=Pattern.compile("R*CWT");
	//������1
	Pattern m_p3=Pattern.compile("W+R+");
	//������1
	Pattern m_p4=Pattern.compile("R+W+");
	
	Pattern b_p1=Pattern.compile("MR*CWTR+M");
	
	//������
	Pattern b_p2=Pattern.compile("MR*CWTM");
	
	//������1
	Pattern b_p3=Pattern.compile("MW+R+M");
	
	//������1
	Pattern b_p4=Pattern.compile("MR+W+M");
	
	public String match();
	
}
