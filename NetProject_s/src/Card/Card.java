package Card;

/*
 * 카드의 정보가 담긴 클래스입니다.
 */
public class Card {
	private String Shape;//카드 이미지 위치
	private int num;//카드 번호
	
	/*
	 * 특수한 세팅(or 테마)의 카드를 사용합니다.
	 */
	public Card(String Shape,int num){
		this.Shape = Shape;
		this.num = num;
	}
	
	/*
	 * 기본 세팅의 카드(Default_Card)를 사용합니다.
	 */
	public Card(int num){
		if(num >= 20){ //카드는 0~19번이기 때문에 20이상은 에러가 납니다.
		}else{
			this.num = num;
			this.Shape = CardInfo.default_Shape[num];
		}
	}
	
	public String getShape() {
		return Shape;
	}
	
	public int getNum() {
		return num;
	}
	
	
	
}
