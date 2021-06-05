package rwlockrefactoring.nfa;

import java.util.regex.Pattern;

public interface RefactoringString {
	Pattern m_p1 = Pattern.compile("R*CWTR+");
	// lock splitting
	Pattern m_p2 = Pattern.compile("R*CWT");
	// lock downgrading
	Pattern m_p3 = Pattern.compile("W+R+");
	// lock splitting
	Pattern m_p4 = Pattern.compile("R+W+");

	Pattern m_p5 = Pattern.compile("R+W+|W+R+");
	Pattern b_p1 = Pattern.compile("MR*CWTR+M");

	// lock splitting
	Pattern b_p2 = Pattern.compile("MR*CWTM");

	// lock downgrading
	Pattern b_p3 = Pattern.compile("MW+R+M");

	// lock splitting
	Pattern b_p4 = Pattern.compile("MR+W+M");

	public String match();

}
