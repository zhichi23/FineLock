package rwlockrefactoring.nfa;

public class DFA {
	
	String inputChar;     //Ҫƥ����ַ���
	String charracter;        //�ַ���   
	int stateNum = 5;           //״̬������Ĭ��Ϊ0
	int currState = 1;          //��ʼ״̬��ţ�Ĭ��Ϊ1
	int endNum = 1;             //����״̬������Ĭ��Ϊ1
	int acceptState[]=new int[endNum];;    //����״̬
	int convertFrom[][] = {{1,2,-1,-1},{2,-1,3,-1},{3,-1,3,4},{5,-1,-1,-1},{5,-1,-1,-1}};   //ת����
	

	
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
			boolean isExist =false; //�����һ���ַ��Ƿ����ַ�����
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