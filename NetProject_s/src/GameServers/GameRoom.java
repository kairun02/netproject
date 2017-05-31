package GameServers;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;


/*
 * Game방 내부를 담당하는 클래스입니다.
 */
public class GameRoom {
	public static final int MAX_GAMEUSER = 4; //최대 게임 유저는 4명
	public static final int MAX_CARD = 3; //최대 카드 수는 3장
	public static final int BASIC_RATE = 100; //기본 요금(임의로 100원으로 설정했습니다. 바꿔주세요)
	
	//생성자입니다. 메인서버와 자신의 방번호를 받아옵니다.(완)
	public GameRoom(int num){
		room_number = num;
		playuser = new GameUser[MAX_GAMEUSER];
		sendbomb = ByteBuffer.allocate(1024);
	}
	
	public GameRoom(int roomnum, int maxuser){
		room_number = roomnum;
		playuser = new GameUser[maxuser];
		sendbomb = ByteBuffer.allocate(1024);
	}
	
	//방에 유저를 추가합니다.(완)
	public boolean addUser(User u){
		//최대 인원일때 추가 불가
		if(num_user>=MAX_GAMEUSER){ return false; }
		playuser[num_user++] = new GameUser(u);
		refreshRoom(); //방을 갱신해줍니다.
		return true;
	}
	
	/*
	 * 베팅을 합니다. - 미완
	 */
	public boolean Betting(GameUser user, int num){
		if(user == null){ return false; }
		if(user.isAlive()){ //살아있을 때
			int type = user.Betting(num, gameplaying.part_budget); //어떤 타입의 베팅을 했는 지 받아옵니다.
			switch(type){
			case -404: return false; //에러
			case -1: //die
				break;
			case 0: //call
			case 1: //half
				break;
			}
		}
		return true;
	}
	
	// 최종 덱 결정
	public boolean SelectFinalDeck(GameUser user, int n1, int n2){
		
		return true;
	}
	
	// 방에서 유저를 제거합니다(방을 나가다)(완)
	public boolean deleteUser(SocketChannel s){
		int i;
		for(i=0;i<num_user;i++){
			if(playuser[i].equal_Socket(s)){ break; }
		}
		//Not Found
		if(i==num_user){ return false; }
		//유저 목록에서 제거 후 그 유저부터 뒷 유저들을 자리이동 시켜줍니다.
		num_user--;
		for(int j=i;j<num_user;j++){
			playuser[j] = playuser[j+1];
			playuser[j].setNum(j);
		}
		playuser[num_user] = null;
		refreshRoom();
		return true;
	}
	
	/*
	 * 조건이 충족됬을 때, 게임을 시작합니다.
	 * 만약 조건이 충족되지 않았을 경우 왜 시작을 할 수 없는지 메세지를 보내줍니다.
	 */
	public boolean startGame(SocketChannel s){
		try {
			if(num_user == 1){ //방안에 유저가 한명일 경우
				byte[] b = {5,2,1}; //5(Game) - 2(Start) - 1(Fail-player is too few)
				sendbomb.put(b);
				sendbomb.flip();
				s.write(sendbomb);
				sendbomb.clear();
				return false;
			}
			for(int i=1;i<num_user; i++){ //방장을 제외한 방에 있는 유저가 전부 레디상태인지 체크
				if(!playuser[i].isReady()){ //레디상태가 아닌 유저가 하나라도 있을 경우
					byte[] b = {5,2,2}; //5(Game) - 2(Start) - 2(Fail-not ready player is being)
					sendbomb.put(b);
					sendbomb.flip();
					s.write(sendbomb);
					sendbomb.clear();
					return false;
				}
			}
		}catch (IOException e) { }
		playuser[0].changeReady(); //게임 시작 초기화를 할 때, 레디 상태를 false로 변경해주기 위해 미리 true로 맞춰둡니다.(방장)
		gamestart = true; //유저가 2명이상이고 전부 레디일 경우 게임을 시작합니다.
		byte[] b = {5,2,0}; //5(Game) - 2(Start) - 0(Success)
		sendbomb.put(b);
		sendbomb.flip();
		sendAllPlayer(sendbomb); //모든 유저한테 게임이 시작했다고  알립니다.
		gameplaying = new Play(this, num_user); //플레이 쓰레드 생성
		sendbomb.clear();
		return true;
	}
	
	/*
	 * 소켓 채널로 유저를 판별합니다.
	 * 못찾으면 null을 리턴합니다.(완)
	 */
	public GameUser getUser(SocketChannel s){
		for(int i =0;i<num_user;i++){
			if(playuser[i].equal_Socket(s)){ return playuser[i]; }
		}
		return null;
	}
	
	// 유저의 순서로 유저를 반환합니다.
	public GameUser getUser(int num){
		if(num >= num_user){ return null; }
		return playuser[num];
	}
	
	/*
	 * 방이 살아있는지의 여부를 보내줍니다.(완)
	 * 메인에서 방들을 갱신할 때 사용합니다.
	 */
	public boolean isAlive(){
		//유저가 없으면 사라져야합니다.
		if(num_user <= 0){ return false; }
		else{ return true; }
	}
	
	// game이 시작했는지 알려줍니다(완)
	public boolean isStarted(){ return gamestart; }
	
	/*
	 * 카드를 오픈합니다.
	 */
	public boolean openCard(GameUser user, int num){
		int open_num;
		if(num == -1){ //Timeout입니다
			open_num = user.openCard(0, false);
		}else{
			open_num = user.openCard(num, true);
		}
		if(open_num == -1){ return false; } //open실패
		sendbomb.putInt(user.getNum()); //유저 순번
		sendbomb.putInt(open_num); //오픈된 카드 번호
		return true;
	}
	
	/*
	 * 메세지를 읽고 해당하는 이벤트를 발생시킵니다.
	 */
	public void readEvent(SocketChannel s, ByteBuffer buf){
		byte b = buf.get();
		switch(b){
		case 0: // ready/start
			if(getUser(s).getNum() == 1){ //방장일 경우
				//게임이 시작되지 않았고, 게임 시작에 성공햇을 경우
				if(!gamestart && startGame(s)){ gameplaying.start(); }
			}else{ //일반유저일 경우
				getUser(s).changeReady(); //레디상태를 바꿔줍니다.
			}
			break;
		case 1: // 패를 공개합니다.
			int num = buf.getInt();
			if(openCard(getUser(s), num)){ gameplaying.getRecieve(getUser(s).getNum()); }
			break;
		case 2: // 판돈을 겁니다.
			int bet_type = buf.getInt();
			if(Betting(getUser(s), bet_type)){ gameplaying.getRecieve(getUser(s).getNum()); }
			break;
		case 3: // 최종 패를 선택합니다.
			break;
		case 4: // 방을 나갑니다.
			User u = getUser(s);
			deleteUser(s); //유저 제거
			u.setRoomNumber(-1); //위치를 대기실로 변경
			break;
		}
	}
	
	// 갱신된 방 정보를 방 내부 유저들에게 보내줍니다.
	public void refreshRoom(){
		ByteBuffer room_info = ByteBuffer.allocate(1024);
		byte[] b = {5,1}; //5(Game) - 1(Refresh)
		room_info.put(b);
		room_info.putInt(num_user); //유저 수
		for(int i=0;i<num_user;i++){
			room_info.putInt(playuser[i].getNickname().length()); //닉네임의 길이
			room_info.put(playuser[i].getNickname().getBytes()); //닉네임
			room_info.putInt(playuser[i].getBudget()); //판돈
			room_info.put((byte)(playuser[i].isReady()?1:0)); //레디 상태  1=레디, 0=레디x
			//아바타를 넣어줍니다.(미구현)
		}
		sendAllPlayer(room_info);
		room_info.clear();
	}
	
	/*
	 * 게임 결과를 나타내는 함수입니다.
	 * 승자의 번호, 벌어들인 돈, 케이스를 나타냅니다.
	 */
	public void resultGame(int winner, int money, int end_case){
		switch(end_case){
		case Play.CASE_ALL_DIE: //1명빼고 다 죽음
			break;
		case Play.CASE_DEFAULT_WIN: //평범한 승리
			break;
		}
	}
	
	/*
	 * 플레이하는 모든 플레이어한테 해당 Action(buffer)을 보내줍니다.
	 * 보통 플레이어의 액션(카드공개, 판돈걸기)을 받았을 때, 보내줍니다.
	 */
	public void sendAllPlayer(ByteBuffer send){
		for(int i=0;i<num_user;i++){
			try {
				playuser[i].getSocket().write(send);
				send.rewind();
			} catch (IOException e) {
			}
		}
	}
	
	// 해당 번호의 유저한테 해당 카드를 전달해줍니다.
	public void sendCard(int target, int card){ playuser[target].getCard(card); }
	
	private int num_user=0;
	private int room_number; //방번호
	private GameUser[] playuser;
	private boolean gamestart = false; //게임 시작여부
	private Play gameplaying = null;
	public ByteBuffer sendbomb;
}

/*
 * 게임 서버에서 사용할 유저정보입니다.
 */
class GameUser extends User{
	
	//유저 정보를 받아서 기록합니다.
	public GameUser(User u){
		super(u);
		my_card = new int[GameRoom.MAX_CARD]; //최대카드 수로 초기화
	}
	
	//받은 타입에 따라서 베팅을 해줍니다.
	public int Betting(int type, int order){
		switch(type){
		case -1: //다이
			alive = false;
			break;
		case 0: // 콜
			if(order > budget){ //올인
				payment = budget;
			} else{ payment = order; }
			break;
		case 1: //하프
			payment = order;
			break;
		case 2: //따당
			break;
		case 3: //check
			break;
		case 4: //삥
			break;
		default:
			return -404;
		}
		return type;
	}
	
	// ready상태 변경
	public void changeReady(){ ready = !ready; }
	
	// 카드를 받습니다. 그리고 받은 카드를 client에 보내줍니다.
	public void getCard(int n){
		try {
			my_card[num_card++] = n;
			byte[] b = {5, 3}; //5(Game) - 3(send Card)
			ByteBuffer send = ByteBuffer.allocate(1024);
			send.put(b);
			send.putInt(n); //카드 번호
			send.flip();
			my_channel.write(send);
			send.clear();
		} catch (IOException e) {}
	}
	
	//내 번호(순서)를 알려줍니다.
	public int getNum(){ return my_num; }
	
	//내 점수를 알려줍니다.
	public int getRank(){ return my_rank; }
	
	// 살아있는지 물어봅니다.(완)
	public boolean isAlive(){ return alive; }
	
	// 올인했는지 물어봅니다.(완)
	public boolean isAllin(){ return (budget==payment?true:false); }
	
	/*
	 * 받은 데이터가 실제로 소유중인 카드인지를 물어봅니다.
	 * (클라이언트에서 데이터를 조작할 수 있으므로)
	 */
	public boolean isHave(int a){
		for(int i=0;i<num_card;i++){
			if(my_card[i]==a) return true;
		}
		return false;
	}
	
	// ready상태 확인(완)
	public boolean isReady(){ return ready; }
	
	/*
	 * 카드를 오픈합니다.
	 * return값은 open_card로 설정된 카드입니다.(완)
	 */
	public int openCard(int num, boolean success){
		if(success && isHave(num) && open_card == -1){ //timeout이 아닐때 + 카드가 존재할때 + 카드를 오픈하지 않았을때
			open_card = num;
		}else if(!success && open_card == -1){ //timeout일때
			open_card = my_card[num];
		}
		return open_card;
	}
	
	//게임을 시작할 때, 유저의 상태를 초기화 해줍니다.(완)
	public void resetGame(){
		for(int i=0; i<GameRoom.MAX_CARD;i++){ //카드 초기화
			my_card[i] = -1; //-1은 아직 받지 않았다는걸 뜻합니다.
		}
		num_card = 0;
		open_card = -1;
		alive = true; //살아있는 상태
		payment = GameRoom.BASIC_RATE; //기본 요금을 지불하고 시작합니다.
		my_rank = -404;
	}
	
	// 방에서의 내 번호(순서)를 세팅해줍니다.(완)
	public void setNum(int n){ my_num = n; }
	
	// rank를 세팅해줍니다.
	public void setRank(int n){ my_rank = n; }
	
	private int[] my_card; //가지고 있는 카드 정보.
	private int num_card; //가지고 있는 카드 갯수.
	private int my_num; //게임 내 내 번호(1-방장)
	private int my_rank; //내 점수
	private boolean ready=false; //ready상태
	private boolean alive; //살았는지 죽었는지
	private int open_card; //오픈한 카드 번호
	private int payment; //내가 지불한(할) 돈
}
