package GameServers;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import Card.CardDeck;
import Card.CardInfo;

/*
 * 게임을 플레이 하는 클래스입니다.
 * 플레이 흐름과 승패를 담당하고있습니다.
 */
public class Play extends Thread {
	public final static int CASE_ALL_DIE = 0; //1명 제외 전부 죽음
	public final static int CASE_DEFAULT_WIN = 1; //일반적인 승리
	
	/*
	 * 게임방 정보(유저 정보등은 방이 가지고 있습니다)와 유저수를 넣어줍니다.
	 */
	public Play(GameRoom playroom, int num){
		myroom = playroom;
		num_user = num;
		timer = new GameTimer[num_user]; // 유저 수만큼의 타이머 생성.
		round = 0;
		all_budget = GameRoom.BASIC_RATE*num_user; //기본 게임료
		part_budget = GameRoom.BASIC_RATE;
	}
	
	//죽은 사람을 증가시킵니다.
	public void addDied(){ die++; }
	
	/*
	 * 점수 비교로 승자를 찾아냅니다.
	 * 특수경우 판별을 아직 못함.
	 */
	public int findDefaultWinner(){
		int[] alive_player = new int[call];
		int[] rank = new int[call];
		int num=0;
		for(int i=0; i<num_user; i++){
			if(myroom.getUser(i).isAlive()){
				alive_player[num] = i;
				rank[num++] = myroom.getUser(i).getRank();
			}
		}
		int max = 0; //0번째플레이어의 rank값을 기본 맥스치로 둡니다.
		for(int i=1;i<call;i++){
			if(rank[i]<0){ //특수 케이스 체크
				
			}else{
				max = (rank[max]<rank[i]?i:max);
			}
		}
		return max;
	}
	
	/*
	 * 타이머가 끝났음을 알려줍니다.
	 */
	public void endTimer(GameTimer time){
		for(int i=0;i<num_user;i++){
			if(timer[i]==time){
				timer[i] = null;
				break;
			}
		}
	}
	
	/*
	 * 유저 순번을 받아서 해당하는 쓰레드에 전해줍니다.
	 */
	public void getRecieve(int num){
		if(num >= num_user){ return; }
		if(timer[num] != null){ timer[num].getRecieve(); }
	}
	
	/*
	 * 답장을 받지 못했을 때.
	 */
	public void notRecieve(SocketChannel s){
		//외부 패킷일때
		if(myroom.getUser(s)==null){ return; }
		//라운드에 따라서 액션이 달라집니다.
		switch(round){
		case 1: //패 공개
			myroom.openCard(myroom.getUser(s), -1); //-1은 Timeout으로 보내줍니다.
			break;
		case 2: //돈걸기
			myroom.Betting(myroom.getUser(s), -1); //Timeout = die
			break;
		case 3:
			myroom.SelectFinalDeck(myroom.getUser(s), -1, -1); //-1, -1은 Timeout입니다.
			break;
		default: //해당하지않을때
			return;
		}
		count--;
	}

	//call한 사람을 초기화합니다
	public void resetCall(){ call = 1; }
	
	@Override
	public void run(){
		super.run();
		ByteBuffer flow = ByteBuffer.allocate(1024);
		CardDeck playdeck = new CardDeck();
		playdeck.shuffle(); //카드를 섞어줍니다.
		count = 0;
		
		//Round 0 : 패를 돌립니다.
		while(count < 2){
			for(int i=0;i<num_user;i++){
				myroom.sendCard(i, playdeck.getOne().getNum());
			}
			count++;
		}
		count = 0;
		round++;
		
		//Round1 : 공개할 패를 정합니다.
		byte[] head = {5, 4}; //5(Game) - 4(Show Card)
		myroom.sendbomb.put(head);
		myroom.sendbomb.putInt(num_user);//유저 수
		for(int i=0;i<num_user;i++){
			timer[i] = new GameTimer(myroom.getUser(i).getSocket(), this);
			timer[i].start();
			count++;
		}
		while(true){ //전부 공개할 때까지 기다립니다. or 타이머가 초과할때까지(timer에서 no recieve로 잡아줌)
			if(count == 0) break;
		}
		myroom.sendbomb.flip();
		myroom.sendAllPlayer(myroom.sendbomb);
		myroom.sendbomb.clear();
		round++; 

		//Round2 : 베팅을 합니다.
		call = 0;
		int turnuser = 0; //해당 턴인 유저번호
		count = num_user;
		while(true){
			if(turnuser == num_user){ //유저번호 초기화(cycle)
				turnuser = 0;
				count = num_user;
			}else if(!myroom.getUser(num_user).isAlive()){ //죽었을 때
				turnuser++;
				count--;
				continue;
			}
			timer[turnuser] = new GameTimer(myroom.getUser(turnuser).getSocket(), this);
			timer[turnuser].start();
			while(count == num_user-turnuser){ } //count의 값이 변하기 전까지 기다립니다.
			turnuser++;
			if(die == num_user-1){ //한명 빼고 다 죽었을 때
				int i;
				for(i=0;i<num_user;i++){ if(myroom.getUser(i).isAlive()) break; }
				myroom.resultGame(i, all_budget, CASE_ALL_DIE);
				return; //게임 종료.
			}else if(call + die == num_user){ //전부다 call or die일때
				break;
			}
		}
		//Round2.5 : 카드를 나눠줍니다.
		for(int i=0;i<num_user;i++){
			if(!myroom.getUser(i).isAlive()){ continue; }//죽은 사람 제외
			myroom.sendCard(i, playdeck.getOne().getNum());
		}
		round++;
		count = call; //콜한 사람(살아있는 사람)
		int num_final = 0; //마지막까지 살아남은 순번
		//Round3 : 최종 패를 결정합니다.
		byte[] end = {5, 8}; //5(Game) - 8(Final Card)
		myroom.sendbomb.put(end);
		myroom.sendbomb.putInt(num_user);//유저 수
		for(int i=0;i<num_user;i++){
			if(!myroom.getUser(i).isAlive()){ continue; }//죽은 사람 제외
			timer[i] = new GameTimer(myroom.getUser(i).getSocket(), this);
			timer[i].start();
			while(count == call-num_final){ } //count의 값이 변하기 전까지 기다립니다.
			num_final++;
		}
		myroom.sendbomb.flip();
		myroom.sendAllPlayer(myroom.sendbomb);
		myroom.sendbomb.clear();
		//일반적인 승리.
		int winner = findDefaultWinner();
		myroom.resultGame(winner, all_budget, CASE_DEFAULT_WIN);
		
	}
	

	
	private GameTimer[] timer = null; //게임을 원활하게 진행하기위한 타이머
	private GameRoom myroom; //플레이가 진행되는 방
	private int num_user; //유저 수
	public int all_budget; //총 판돈
	public int part_budget; //개인이 지불 해야하는 판돈
	private int round; //notRecieve일때 어떻게 해야할지
	private int count; //체크용
	private int call; //콜 한사람
	private int die;  //die한 사람
}
