 package rwlockrefactoring.util;

public final class RWString {
	public final static String READ_LOCK="";
	public final static String WRITE_LOCK="W";
	//public final static String DOWNGRADE_LOCK="(R*C(((R*WW*R*)|(WW*R*W*))*)TRR*)";
	
	//ͬ������
	public final static String DOWNGRADE_LOCK="R+{W}R+";
	public final static String UPGRADE_LOCK="R+{W}";
	public final static String DOWNGRADE_LOCK_METHOD="W*WR*R";
	public final static String UPGRADE_LOCK_METHOD="A*R*RWW*";
	
	//ͬ����
	public final static String DOWNGRADE_LOCK_BLOCK="MW*WR*RM";
	public final static String UPGRADE_LOCK_BLOCK="MR*RWW*M";
	
	
	//�ʴ�
	public final static String stamp_op="R";
	public final static String stamp_up="R*W+R*";
	public final static String stamp_d="W+R+";
	public final static String stamp_r="R+";
	
	
}
