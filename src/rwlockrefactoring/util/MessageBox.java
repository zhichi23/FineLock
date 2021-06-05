package rwlockrefactoring.util;

public class MessageBox {

	public void print_num(Count count) {
		System.out.println("synchronized method(s):" + count.sy_num + "  " + "splitting:" + count.sy_up_num + "  "
				+ "downgrading:" + count.sy_down_num + "  " + "read:" + count.sy_read_num + "  " + "write:"
				+ count.sy_write_num + " " + "opt:" + count.sy_op_num);
		System.out.println("bu:" + count.sy_n + "  " + "c:" + count.sy_c);
		System.out.println("can't refactor" + count.sy_can_not1);
		System.out.println("condition refactor" + count.sy_can_not2);
		System.out.println("synchronized block(s):" + count.bl_num + "  " + "splitting:" + count.bl_up_num + "  "
				+ "downgrading:" + count.bl_down_num + "  " + "read:" + count.bl_read_num + "  " + "write:"
				+ count.bl_write_num);
	}
}
