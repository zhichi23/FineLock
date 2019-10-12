package rwlockrefactoring.util;

public class MessageBox {
	
	
	public void print_num(Count count) {
		System.out.println("同步方法:" + count.sy_num + "  " + "锁升级:" + count.sy_up_num +
				"  " + "锁降级:" + count.sy_down_num + "  " + "读锁:"
				+ count.sy_read_num + "  " + "写锁:" + count.sy_write_num);
		System.out.println("不能重构"+count.sy_can_not1);
		System.out.println("状态重构"+count.sy_can_not2);
		System.out.println("同步块:" + count.bl_num + "  " + "锁升级:" + count.bl_up_num 
				+ "  " + "锁降级:" + count.bl_down_num + "  " + "读锁:"
				+ count.bl_read_num + "  " + "写锁:" + count.bl_write_num);
	}
}
