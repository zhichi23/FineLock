package rwlockrefactoring.nfa;

public class DFA {
	
	String inputChar;     //要匹配的字符串
	String charracter;        //字符集   
	int stateNum = 5;           //状态个数，默认为0
	int currState = 1;          //开始状态编号，默认为1
	int endNum = 1;             //接受状态个数，默认为1
	int acceptState[]=new int[endNum];;    //接受状态
	int convertFrom[][] = {{1,2,-1,-1},{2,-1,3,-1},{3,-1,3,4},{5,-1,-1,-1},{5,-1,-1,-1}};   //转换表
	

	
	public DFA(String rex) {
		inputChar=rex;
		charracter="RCWT";
		acceptState[0]=5;
	}

	public boolean makedfa() {
		
		
		int num=0;
		char[] inputChars=inputChar.toCharArray();
		char[] charracters =charracter.toCharArray();
		while(num<inputChar.length()) {
			int y=0;
			boolean isExist =false; //检查下一个字符是否在字符集中
			for(;y<charracters.length;y++) {
				if(inputChars[num]==charracters[y]) {
					isExist=true;
					break;
				}else if(y==charracters.length-1 && charracters[y]=='?') {
					isExist=true;
					break;
				}
			}
			if(isExist) {
				if(convertFrom[currState - 1][y] > 0) {
					currState = convertFrom[currState - 1][y];
				}else {
					return false;
				}
			}else {
				return false;
			}
			num++;
		}
		boolean notAcceptState = true;
		for (int i = 0; i < endNum; i++)
	    {
	        if (currState == acceptState[i])
	        {
	            notAcceptState = false;
	        }
	    }
		if (notAcceptState)
	    {
			return false;
	    }
		return true;
	}
	
}