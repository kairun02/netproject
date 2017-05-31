package GameServers;

import java.io.*;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;

/*
 * Client와의 통신을 담당하는 쓰레드입니다.
 * accept와 readMessage를 담당합니다.
 */
public class ConnectThread extends Thread {
	
	@Override
	public void run(){
		super.run();
		Selector selector = conn_server.selector;
		System.out.println("연결이 시작되었습니다.");
		while (true) {
			try {
				selector.select(); //메세지가 들어올 때까지 기다립니다.(Blocking?)
				
				//들어온 메세지들에 대한 iterator를 받아옵니다.
				Iterator<SelectionKey> it = selector.selectedKeys().iterator(); 
				while(it.hasNext()){
					SelectionKey key = (SelectionKey) it.next();
					if(key.isAcceptable()){ //클라이언트가 접속 요청을 했을 때
						conn_server.accept(key);
					} else if(key.isReadable()){ //Client에서 무언갈 보냈을 때
						conn_server.readMessage(key);
					}
					it.remove(); //실행한 이벤트는 삭제
				}	
			} catch (IOException e) { }
		}
	}
	
	public ConnectThread(Server s){
		super();
		conn_server = s;
	}
	
	private Server conn_server;
}
