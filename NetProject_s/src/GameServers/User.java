package GameServers;

import java.nio.channels.SocketChannel;

/*
 * 로그인 했을 때의 유저를 나타냅니다.
 * 유저 정보와 현재 접속해있는 채널 정보를 가지고 있습니다.
 */
public class User {
	public User(){ }
	
	public User(SocketChannel conn_client){ my_channel = conn_client; }
	
	/*
	 * User정보를 그대로 옮겨줍니다.
	 */
	public User(User u){
		nickname = u.nickname;
		budget = u.budget;
		my_channel = u.my_channel;
		gender = u.gender;
		roomnum = u.roomnum;
	}
	
	// 포트가 같은지 비교합니다.
	public boolean equal_Socket(SocketChannel p){ return (p==my_channel?true:false); }
	
	// 승리 보상 등으로 돈을 획득합니다.
	public void gainBudget(int money){ budget += money; }
	
	// 돈이 얼마 있는지 받아옵니다.
	public int getBudget(){ return budget; }
	
	// 닉네임을 받아옵니다.
	public String getNickname(){ return nickname; }
	
	// 방번호를 받아옵니다.
	public int getRoomNumber(){ return roomnum; }
	
	// 돈을 지불합니다.
	public void payBudget(int money){ budget -= money; }
	
	// 소켓을 받아옵니다.
	public SocketChannel getSocket(){ return my_channel; }
	
	// 방번호를 세팅합니다.
	public void setRoomNumber(int n){ roomnum = n; }
	
	
	private String nickname; //닉네임
	protected int budget; // 가지고 있는 판돈
	protected SocketChannel my_channel; //현재 연결된 소켓
	private int gender; // 성별(아바타용)
	private int roomnum = -1; //방번호(대기실은 -1)
}
