package Card;

import java.util.ArrayList;
import java.util.Collections;

/*
 * 게임에서 사용할 카드 덱입니다.
 */
public class CardDeck {
	
	/* 
	 * 기본 세팅의 새로운 카드덱을 만듭니다.
	 */
	public CardDeck(){
		deck = new ArrayList<Card>();
		Card addCard;
		for(int num=0;num<20;num++){ // 덱에 20장의 카드를 넣는다.
			addCard = new Card(num);
			deck.add(addCard);
		}
	}
	
	/*
	 * 이미 세팅되어있는 덱을 사용합니다.
	 */
	public CardDeck(ArrayList<Card> setDeck){
		deck = setDeck;
	}
	
	/*
	 * 덱을 섞는 함수입니다.
	 */
	public void shuffle(){
		Collections.shuffle(deck); // Collections에 있는 셔플 함수를 가져와서 쓴다.
	}
	
	/*
	 * 카드를 뽑는 함수입니다. 맨위에 카드를 뽑아서 반환해주고 기존의 덱에 카드를 제거합니다.
	 * 만약 카드가 전부 소진 되었다면 null을 리턴합니다.
	 */
	public Card getOne(){
		Card rtnCard=null;
		if(deck.size()!=0){
			rtnCard = deck.remove(0);
		}
		return rtnCard;
	}
	
	/*
	 * 덱의 사이즈를 반환합니다.
	 */
	public int size(){
		return deck.size();
	}
	
	private ArrayList<Card> deck; //Card를 담는 CardArrayList
	
}