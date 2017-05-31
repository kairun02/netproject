package GameServers;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Date;

/*
 * 원활한 게임을 위해서 일정 시간동안 입력이 없으면 서버에서 알아서 처리하기 위함.
 */
public class GameTimer extends Thread {
	public static final long waiting_time = 20*1000L; //20초제한
	public GameTimer(SocketChannel user, Play play) {
		super();
		target = user;
		myplay = play;
	}
	
	@Override
	public void run(){
		super.run();
		long s_time = new Date().getTime(); //시작 시간을 받아옵니다.
		long f_time = 0L;
		byte[] b = {5, 7}; // 5(Game) - 7(Time Limit)
		ByteBuffer send = ByteBuffer.allocate(1024);
		while(f_time < waiting_time || recieve){
			try { //시간에 따라서 1,2,3초 남았다고 보내줍니다.
				if(waiting_time - f_time < 1){
					send.put(b);
					send.put((byte)1);
					send.flip();
					target.write(send);
					send.clear();
				}else if(waiting_time - f_time < 2){
					send.put(b);
					send.put((byte)2);
					send.flip();
					target.write(send);
					send.clear();
				}else if(waiting_time - f_time < 3){
					send.put(b);
					send.put((byte)3);
					send.flip();
					target.write(send);
					send.clear();
				}
			}catch (IOException e) {
			}
			f_time = System.currentTimeMillis() - s_time;
		}
		if(f_time>waiting_time){ //답장을 못받았을 경우
			myplay.notRecieve(target);
			try{
				send.put(b);
				send.put((byte)0); //Timeover
				send.flip();
				target.write(send);
				send.clear();
			}catch( Exception e){ }
		}
		myplay.endTimer(this);
	}
	
	public void getRecieve(){ recieve = true; }
	
	private SocketChannel target;
	private Play myplay;
	private boolean recieve = false; //받았는 지
}
