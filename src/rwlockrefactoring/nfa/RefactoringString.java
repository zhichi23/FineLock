package rwlockrefactoring.nfa;

import java.util.regex.Pattern;

public interface RefactoringString {
	Pattern m_p1=Pattern.compile("R*CWTR+");
	//ËøÉı¼¶
	Pattern m_p2=Pattern.compile("R*CWT");
	//Ëø½µ¼¶1
	Pattern m_p3=Pattern.compile("W+R+");
	//ËøÉı¼¶1
	Pattern m_p4=Pattern.compile("R+W+");
	
	Pattern b_p1=Pattern.compile("MR*CWTR+M");
	
	//ËøÉı¼¶
	Pattern b_p2=Pattern.compile("MR*CWTM");
	
	//Ëø½µ¼¶1
	Pattern b_p3=Pattern.compile("MW+R+M");
	
	//ËøÉı¼¶1
	Pattern b_p4=Pattern.compile("MR+W+M");
	
	public String match();
	
}
