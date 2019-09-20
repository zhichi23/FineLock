package rwlockrefactoring.util;

public class Count {
	
	 // 统计同步方法
	public int sy_num = 0;
	public int sy_up_num = 0;
	public int sy_down_num = 0;
	public int sy_read_num = 0;
	public int sy_write_num = 0;
	//统计同步块
	public int bl_num = 0;
	public int bl_up_num = 0;
	public int bl_down_num = 0;
	public int bl_read_num = 0;
	public int bl_write_num = 0;
	
	public int getSy_num() {
		return sy_num;
	}
	public void setSy_num(int sy_num) {
		this.sy_num = sy_num;
	}
	public int getSy_up_num() {
		return sy_up_num;
	}
	public void setSy_up_num(int sy_up_num) {
		this.sy_up_num = sy_up_num;
	}
	public int getSy_down_num() {
		return sy_down_num;
	}
	public void setSy_down_num(int sy_down_num) {
		this.sy_down_num = sy_down_num;
	}
	public int getSy_read_num() {
		return sy_read_num;
	}
	public void setSy_read_num(int sy_read_num) {
		this.sy_read_num = sy_read_num;
	}
	public int getSy_write_num() {
		return sy_write_num;
	}
	public void setSy_write_num(int sy_write_num) {
		this.sy_write_num = sy_write_num;
	}
	public int getBl_num() {
		return bl_num;
	}
	public void setBl_num(int bl_num) {
		this.bl_num = bl_num;
	}
	public int getBl_up_num() {
		return bl_up_num;
	}
	public void setBl_up_num(int bl_up_num) {
		this.bl_up_num = bl_up_num;
	}
	public int getBl_down_num() {
		return bl_down_num;
	}
	public void setBl_down_num(int bl_down_num) {
		this.bl_down_num = bl_down_num;
	}
	public int getBl_read_num() {
		return bl_read_num;
	}
	public void setBl_read_num(int bl_read_num) {
		this.bl_read_num = bl_read_num;
	}
	public int getBl_write_num() {
		return bl_write_num;
	}
	public void setBl_write_num(int bl_write_num) {
		this.bl_write_num = bl_write_num;
	}
	
	

}
