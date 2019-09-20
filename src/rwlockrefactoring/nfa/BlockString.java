package rwlockrefactoring.nfa;

import java.util.regex.Matcher;

public class BlockString implements RefactoringString{
Matcher m1,m2, m3,m4;
	
	public BlockString(String rws1,String rws2) {
		// TODO Auto-generated constructor stub
		m1=b_p1.matcher(rws1);
		m2=b_p2.matcher(rws1);
		
		m3=b_p3.matcher(rws2);
		
		m4=b_p4.matcher(rws2);
	}

	@Override
	public String match() {
		// TODO Auto-generated method stub
		if (m1.matches()) {
			return "D";
		} else if(m3.matches()) {
			return "DS";
		}else if (m2.matches()) {
			return "U";
		} else if(m4.matches()) {
			return "US";
		}
		return null;
	}
}
