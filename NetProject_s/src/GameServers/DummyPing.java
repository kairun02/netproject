package GameServers;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;

/*
 * Dummy를 주기적으로 보내서 Timeout이 된 클라이언트들과의 연결을 끊습니다.
 * (너무 연결이 느려서 게임에 지장이 있거나, 연결이 끊겼지만 끝났다는 메세지를 받지 못했을 경우를 상정)
 */
public class DummyPing implements Runnable {

	public DummyPing(LinkedList<SocketChannel> clients, Server mains) {
		all_client = clients;
		mainServer = mains;
	}
	
	@Override
	public void run() {
		if(all_client.size() == 0){ //현재 연결된 클라이언트가 0이면 보낼 필요가 없다.
			return;
		}
		
		// 모든 연결된 클라이언트를 저장합니다.
		for(SocketChannel key : all_client){
			faillist.add(key);
		}
		
		long s_time = new Date().getTime(); //시작 시간을 받아옵니다.
		long f_time = 0L; //체크할 시간입니다.
		
		ByteBuffer dummyPing = ByteBuffer.allocate(1024);
		dummyPing.put((byte)8); //8(Dummy)
		dummyPing.flip();
		//연결된 모든 client에게 dummy를 보냅니다.
		for(SocketChannel dest : all_client){
			try {
				dest.write(dummyPing);
				dummyPing.rewind();
			} catch (IOException e) {
			}
		}
		dummyPing.clear();
		
		//Timeout
		while(f_time < Server.TIMEOUT){
			f_time = System.currentTimeMillis() - s_time; // 얼마나 경과했는지 갱신합니다.
		}
		
		// Timeout이 될 때까지 faillist에 속한 애들은 전부 연결을 끊습니다.
		for(SocketChannel cf : faillist){
			mainServer.close_SocketChannel(cf);
		}
		faillist.clear();
		// 쓰레드가 끝났음을 메인서버에 보내줍니다.
		mainServer.endDummy();
	}
	
	/*
	 * Dummy에 대한 답장이 도착하면 faillist에서 해당 클라이언트를 제거합니다.
	 */
	public void receiveAck(SocketChannel ack_channel){
		if(faillist.contains(ack_channel)){ //해당 channel이 list에 존재하면 제거
			faillist.remove(ack_channel);
		}
	}
	
	private LinkedList<SocketChannel> all_client;
	private ArrayList<SocketChannel> faillist;
	private Server mainServer;
}