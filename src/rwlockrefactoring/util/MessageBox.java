package rwlockrefactoring.util;

public class MessageBox {
	
	
	public void print_num(Count count) {
		System.out.println("ͬ������:" + count.sy_num + "  " + "������:" + count.sy_up_num +
				"  " + "������:" + count.sy_down_num + "  " + "����:"
				+ count.sy_read_num + "  " + "д��:" + count.sy_write_num);
		System.out.println("�����ع�"+count.sy_can_not1);
		System.out.println("״̬�ع�"+count.sy_can_not2);
		System.out.println("ͬ����:" + count.bl_num + "  " + "������:" + count.bl_up_num 
				+ "  " + "������:" + count.bl_down_num + "  " + "����:"
				+ count.bl_read_num + "  " + "д��:" + count.bl_write_num);
	}
}
