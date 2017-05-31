package GameServers;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/*
 * 메인 서버로 쓰고 있습니다.
 * 클라이언트와의 연결, 통신을 담당하고 있습니다.
 */
public class Server {
	//상수 설정
	static final int MAX_CLIENT_CONNECT = 20; //연결할 수 있는 최대 클라이언트 수 
	static final int SERVER_PORT = 34152; //서버에서 사용할 포트
	static final int MAX_GAMEROOM = 10; //최대 게임 방 수
	static final long TIMEOUT = 10*1000; //더미를 보냈을 때, 답장이 10초간 안오면 연결이 끊겼다고 판단합니다.
	static final long CHECK_GAMEROOM_TIME = 1*1000; //1초마다 게임방이 존재하는 지 체크합니다.
	static final long SEND_DUMMYPING_TIME = 30*1000; //30초마다 dummyping을 보냅니다.
	
	//main
	public static void main(String args[]) throws Exception{
		Server mainServer = new Server(SERVER_PORT);
		Thread connecting = new ConnectThread(mainServer);
		connecting.start();
		
		long roomcheck_start_time = new Date().getTime(); //시작 시간을 받아옵니다.
		long roomcheck_refresh_time = 0L; //방 갱신을 체크할 시간입니다.
		long dummy_start_time = new Date().getTime(); //더미 핑 start
		long dummy_refresh_time = 0L;
		//서버를 끌 때까지 돈다.
		while(true){
			if(mainServer.isEnd()){
				break;
			}
			//살아있는 게임방이 있다면 1초마다 살아있는지 체크 요청을 합니다.
			if(roomcheck_refresh_time >= CHECK_GAMEROOM_TIME){
				mainServer.refresh_GameRoom(); //갱신
				roomcheck_start_time = System.currentTimeMillis();
			}
			//30초마다 Dummy를 보냅니다.
			if(dummy_refresh_time >= SEND_DUMMYPING_TIME){
				mainServer.send_Dummy();
				dummy_start_time = System.currentTimeMillis();
			}
			
			// 얼마나 경과했는지 갱신합니다.
			roomcheck_refresh_time = System.currentTimeMillis() - roomcheck_start_time; 
			dummy_refresh_time = System.currentTimeMillis() - dummy_start_time;
		}
		
		mainServer.closeServer();
	}
	
	/*
	 * 서버 생성자. mainServer입니다. port번호를 받아서 서버를 생성합니다.
	 * 채널과 selector를 열고, 게임 방 연결 등을 초기화시켜줍니다.(완)
	 */
	public Server(int port){
		my_port = port;
		try{
			mainchannel = ServerSocketChannel.open();
			mainchannel.configureBlocking(false); // Non-Block 통신을 위함.
			my_socket = mainchannel.socket();
			my_socket.bind(new InetSocketAddress(my_port)); // binding
			
			selector = Selector.open();
			mainchannel.register(selector, SelectionKey.OP_ACCEPT); //accept를 맡깁니다.
			
			System.out.println("Server Creat Success!!");
			active_users = new HashMap<SocketChannel, User>();
			active_client = new LinkedList<SocketChannel>();
			gameroom = new GameRoom[MAX_GAMEROOM];
			alive_gameroom = new boolean[MAX_GAMEROOM];
			
			//초기화
			for(int i=0;i<MAX_GAMEROOM;i++){
				gameroom[i] = null;
				alive_gameroom[i] = false;
			}
		}catch(Exception e){
			e.printStackTrace();
			System.out.println("서버 생성 실패!!");
		}
		
		System.out.println(""); //Test-Create Success
	}
	
	/*
	 * accept요청을 받아들여줍니다.(완)
	 */
	public boolean accept(SelectionKey key){
		ServerSocketChannel server = (ServerSocketChannel)key.channel();
		SocketChannel sc;
		try {
			sc = server.accept();
			if(sc==null){ //accept한 클라이언트의 데이터가 없을 경우
				System.out.println("연결할 client를 찾지 못했습니다.");
				return false;
			}
			//서버가 full일때 클라이언트를 거부하고, 왜 거부당했는지 보내줍니다.
			if(num_connect_client >= MAX_CLIENT_CONNECT){ 
				ByteBuffer buf = ByteBuffer.allocate(1024);
				byte[] b = {1, 0}; //Join(1) fail-Server full(0)
				buf.put(b);
				sc.write(buf); //접속 실패 이유를 전송합니다.
				sc.close(); //채널을 닫습니다.
				return false;
			}
			sc.configureBlocking(false); // Non-Blocking
			sc.register(selector, SelectionKey.OP_READ); //해당 채널에서 받는 read이벤트를 추가합니다.
			add_SocketChannel(sc);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		return true;
	}
	
	/*
	 * list에 클라이언트의 정보를 저장합니다.(완)
	 */
	public boolean add_SocketChannel(SocketChannel client){
		if(!active_client.add(client)) return false;
		else return true;
	}
	
	/*
	 * Server End (완)
	 */
	public void closeServer() throws IOException{
		System.out.println("서버를 종료합니다.");
		my_socket.close();
		mainchannel.close();
	}
	
	/*
	 * 소켓채널을 닫습니다. 해당 클라이언트와의 연결이 완전히 종료되었을 때만 부릅니다.(완)
	 * 만약 로그인상태라면 해당하는 user도 로그아웃시켜줍니다.
	 */
	public boolean close_SocketChannel(SocketChannel sc){
		if(!sc.isOpen()){ //이미 닫혀있으면 빠져나감
			return false;
		}
		if(active_users.containsKey(sc)){
			logout(sc); //접속중이라면 로그아웃 시켜줍니다.
		}
		num_connect_client--;
		try {
			active_client.remove(sc); //List에서 제거
			sc.close(); //서버에서도 연결 끊기
		} catch (IOException e) {
			e.printStackTrace();
		}
		return true;
	}

	/*
	 * 유저를 생성합니다.(회원가입)
	 */
	public boolean createUser(SocketChannel client, ByteBuffer buf){
		String createID, createPW; //새로 생성할 아이디와 비밀번호
		// ID를 받아옵니다.
		int len = buf.getInt();
		byte[] ID = new byte[len];
		for(int i = 0; i<len; i++){
			ID[i] = buf.get();
		}
		createID = new String(ID,0,len);
		//PW를 받아옵니다.
		len = buf.getInt(); // PW길이를 받아옵니다.
		byte[] PW = new byte[len];
		for(int i = 0; i<len; i++){
			PW[i] = buf.get();
		}
		createPW = new String(PW,0,len);
		//이 외에 입력할 정보(성별, 나이 등등등)을 똑같은 방식으로 받아온 후 DB랑 연동해서 중복 아이디 체크라던가 부탁드릴게요
		
		return false;
	}
	
	/*
	 * gameroom을 생성합니다.(완)
	 */
	public boolean creatGameRoom(SocketChannel client){
		if(num_gameroom >= MAX_GAMEROOM){ //게임방이 수용량을 넘겼을 경우 생성을 못합니다.
			return false;
		}
		for(int i=0;i<MAX_GAMEROOM;i++){
			if(!alive_gameroom[i]){
				gameroom[i] = new GameRoom(i);
				if(gameroom[i]!=null){ //생성에 성공하면
					gameroom[i].addUser(active_users.get(client)); // 방생성을 요청한 유저를 처음으로 추가
					alive_gameroom[i] = true;
				}
			}
		}
		num_gameroom++;
		return true;
	}
	
	//DummyThread가 끝났음을 알려줍니다.(완)
	public void endDummy(){ dummy = null; }
	
	/*
	 * 방번호를 입력하면 해당하는 방이 살아있는지 체크해서 방정보를 보내줍니다.
	 * 존재하지 않는다면 null을 반환합니다.(완)
	 */
	public GameRoom getGameroom(int n){
		if(alive_gameroom[n] == true){
			return gameroom[n];
		}
		return null;
	}
	
	//서버를 종료해야하는 지 알려줍니다.(완)
	public boolean isEnd(){ return server_end; }
	
	/*
	 * 메세지를 보낸 채널과 메세지(ID, password가 담겨있는)를 받아서 로그인 시도를 합니다.
	 * 이부분은 로그인 맡으신 분이 DB연동으로 확인하는법을 알려주셔야 할 수 있을거같네요.
	 */
	private boolean login(SocketChannel sc, ByteBuffer buf){
		//로그인이 성공했을 경우, 유저 정보를 불러와서 channel을 key로 유저정보를 value로 해서 active_users에 추가해주세요.
		
		return false;
	}
	
	/*
	 * 로그인 상태일 때, 로그아웃 요청을 하면 Active_user에서 제거합니다.(완)
	 */
	private boolean logout(SocketChannel sc){
		if(!active_users.containsKey(sc)){ //로그인 상태가 아닐 경우
			return false;
		}
		active_users.remove(sc); //접속한 유저 목록에서 지워줍니다.
		
		try {
			ByteBuffer send = ByteBuffer.allocate(1024);
			send.put((byte)6); //6(Logout Success) = 정상 로그아웃
			send.flip();
			sc.write(send);
			send.clear();
		} catch (IOException e) {
		}
		return true;
	}
	
	/*
	 * 연결된 클라이언트에서 받은 메세지에 맞는 액션을 취해줍니다.
	 */
	public void readMessage(SelectionKey key){
		//Accept한 SocketChannel에만 read이벤트를 걸어놓았기 때문에 SocketChannel의 메세지입니다.
		SocketChannel client = (SocketChannel)key.channel();
		ByteBuffer buf = ByteBuffer.allocate(1024);
		ByteBuffer send = ByteBuffer.allocate(1024);
		try {
			int read = client.read(buf);
			
			if(read == -1){ //connecting closed
				close_SocketChannel(client); //해당 client를 종료.
			}
			
			byte packet = buf.get(); // 첫 바이트는 어떤 종류의 메세지인지를 나타냅니다.
			
			//로그인 상태에서 실행하는 명령인데 로그인 상태가 아닐경우(error message)
			if(packet>=2 && packet<=6 && !active_users.containsKey(client)){
				buf.clear();
				return;
			}
			
			switch(packet){
			case 0: //Login(로그인)
				if(!login(client, buf)){
					byte[] b = {0,0}; //0(login) - 0(fail)
					send.put(b);
					send.flip();
					client.write(send);
				} else{
					byte[] b = {0,1}; //0(login) - 1(Success)
					send.put(b);
					send.flip();
					client.write(send);
				}
				break;
			case 1: //User Join(회원가입)
				createUser(client, buf); //이 함수에서 성공, 실패에 따른 답장까지 전부 보내줍니다.
				break;
			case 2: //Game Join(게임방 참가)
				if(active_users.get(client).getRoomNumber() != -1){ //대기실이 아닐 경우
					break; //잘못된 패킷입니다.
				}
				int num = buf.getInt(); //방번호를 받아옵니다.
				GameRoom room = getGameroom(num);
				if(room == null){ //방이 존재하지 않을 때
					byte[] b = {1,5}; //1(Join) - 5(Couldn't find room)
					send.put(b);
					send.flip();
				} else if(room.isStarted()){
					byte[] b = {1,4}; //1(Join) - 4(Game already started)
					send.put(b);
					send.flip();
				} else if(!room.addUser(active_users.get(client))){ // 유저 등록이 실패했을 때(방이 꽉찬경우)
					byte[] b = {1,3}; //1(Join) - 3(Room is Full)
					send.put(b);
					send.flip();
				} else{ //성공한경우 < 위 else if에서 addUser를 실행해서 True가 나올때 이쪽문단으로 오게 됩니다.
					byte[] b = {5,0}; //5(Game) - 0(Room Join Success)
					send.put(b);
					send.putInt(num); //방번호
					send.flip();
				}
				client.write(send);
				break;
			case 3: //Game Create(게임방 생성)
				if(!creatGameRoom(client)){ //방 생성에 실패할 경우(gameroom is full)
					send.put((byte)4); //4(Room Create Fail)
					send.flip();
					client.write(send);
				}else{ //방 생성에 성공할 경우
					int room_num = active_users.get(client).getRoomNumber();
					byte[] b = {5, 0}; //5(Game) - 0(Room Join Success)
					send.put(b);
					send.putInt(room_num);//방번호
					send.flip();
					client.write(send);
				}
				break;
			case 4: //Chat(채팅 내용)
				//아직 미구현
				break;
			case 5: //Game(게임 방 내부에서의 이벤트)
				//유저 정보를 받아서 해당 유저가 속해있는 방에 readEvent로 던져줍니다.
				int roomnum = active_users.get(client).getRoomNumber(); //방번호를 받아옵니다.
				if(roomnum==-1 ||!alive_gameroom[roomnum]) break; //대기실이거나 죽은 게임방일 경우 잘못된 패킷이므로 무시합니다.
				gameroom[roomnum].readEvent(client, buf);
				break;
			case 8: //Reply Dummy(더미에 대한 응답)
				if(dummy!=null){ //DummyThread가 살아있을 때
					dummy.receiveAck(client);
				}
				break;
			case 9: //Close Request(서버에 종료요청을 보냄)
				send.put((byte)9); //9(Close reply) 답장을 보내고 종료합니다.
				send.flip();
				client.write(send);
				close_SocketChannel(client);
				break;
			default:
				//에러 패킷입니다.
			}
			//버퍼들을 초기화 해줍니다.
			buf.clear();
			send.clear();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/*
	 * game방 상태를 받아서 방이 죽었으면 사라졌다고 갱신합니다.(완)
	 */
	public void refresh_GameRoom(){
		for(int i=0;i<MAX_GAMEROOM;i++){
			//앞의 조건이 false면 뒷 조건은 검사하지않기 때문에  alive일때만 gameroom에 요청합니다.
			if(alive_gameroom[i] && !gameroom[i].isAlive()){ 
				alive_gameroom[i]=false;
				gameroom[i]=null;
				num_gameroom--;
			}
		}
	}
	
	/*
	 * 더미 핑을 보냅니다.(완)
	 */
	public void send_Dummy(){
		dummy = new DummyPing(active_client, this);
		Thread dumm = new Thread(dummy);
		dumm.start();
	}
	
	
	private int my_port; //서버에 접속하기 위한 포트
	private boolean server_end=false; //서버 종료. true면 종료합니다.
	
	private int num_gameroom=0; // 현존하는 게임방의 수.
	private GameRoom[] gameroom; // 게임방을 가리키는 배열입니다.
	private boolean[] alive_gameroom; // gameroom이 죽었는 지 살았는지 체크합니다.
	private Map<SocketChannel, User> active_users; // 접속중인 유저 리스트. socketChannel이 키, User가 value
	
	private ServerSocketChannel mainchannel; //연락을 받을 메인 채널입니다.
	private ServerSocket my_socket; // main server socket
	public Selector selector; // channel을 관리할 selector
	private int num_connect_client=0; //연결중인 클라이언트의 수
	public LinkedList<SocketChannel> active_client; // 연결중인 클라이언트들을 저장하는 list입니다.
	
	private DummyPing dummy; //Dummy를 보내는 쓰레드(Runnable)
}
