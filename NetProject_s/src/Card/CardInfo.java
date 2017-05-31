package Card;

/*
 * 카드 정보를 가지고 있습니다.
 * CardDeck에서 유저도 가지고 있어야할 정보를 뺐습니다.
 */
public class CardInfo {
	public CardInfo(){}
	
	/*
	 * 족보를 체크하는 함수입니다.
	 * 카드의 번호를 받아옵니다(수정 전 : 카드를 받아옵니다)
	 * 두 번호를 보고 점수를 매겨서 반환해줍니다.
	 * 특수 족보의 경우 음수로 반환해줍니다.
	 */
	public static int scoreCheck(int card1,int card2){
		//
	    //38광땡,18광땡,13광땡(32,31 < 광땡은 30점 이상의 점수  // + 18,13은 같은 높이이므로 같은 점수 처리할게요.
	    //10땡,9땡,8땡,...,1땡(30,29,28,...21) <
	    //알리,구삥,장삥,세륙(19,18,17,16) <
	    //갑오,끗(9,8~0)
		//땡잡이,암행어사,구사(-1,-2-3) <특별한 경우라 음수 처리 해놨습니다.
		int score = 0;
	   	int []card = new int[2];
	   	int []month = new int[2];
	   	
	   	//순서 처리
	   	//에러를 거르고 작은 카드가 card[0]에 큰 카드가 card[1]에 들어가게합니다.
		if(card1==card2 || card1>19 || card2>19){//error
			return -404;
		}else if(card1 > card2){
	   		card[0] = card2;
	   		card[1] = card1;
	   	}else{
	   		card[0] = card1;
	   		card[1] = card2;
	   	}
		
		//월 체크
		//카드의 값이 0~19이기때문에 (n/2)+1의 값이 월이 됩니다.
		month[0] = card[0]/2 + 1;
		month[1] = card[1]/2 + 1;
		
		//광처리(0 - 1광, 4 - 3광, 14 - 8광)
		//작은 카드가 card[0]이기 때문에 card[0]은 1광이나 3광 이여야합니다.
		if((card[0] == 0 || card[0] == 4)&&(card[1] == 4 || card[1] == 14)){
			if(card[0] == 4 && card[1] == 14){ // 38광땡
				score = 32;
			}else{ //13, 18광땡
				score = 31;
			}
		}else if(month[0] == month[1]){ //땡처리 (두 카드의 월이 같은 경우)
			score = month[0] + 20;
		}else if(month[0] == 1){//알리, 구삥, 장삥(전부 작은 수가 1월이여야 합니다.) + 1월과의 조합(끗)
			switch(month[1]){
			case 2: //알리
				score = 19;
				break;
			case 9: //구삥
				score = 18;
				break;
			case 10: //장삥
				score = 17;
				break;
			default: //그외(끗)
				score = (month[0] + month[1]) % 10;
			}
		}else if(month[0] == 4){
			switch(month[1]){
			case 6: //세륙(사육)
				score = 16;
				break;
			case 7: //암행어사(광땡잡이)
				score = -2;
				break;
			case 9: //구사
				score = -3;
				break;
			default: //그외(끗)
				score = (month[0] + month[1]) % 10;
			}
		}else if(month[0] == 3 && month[1] == 7){ //땡잡이
			score = -1;
		}else{ //나머지(끗)
			score = (month[0] + month[1]) % 10;
		}
	   	
	   	return score;
	}
	
	/*
	 * 기본적으로 사용하는 카드의 이미지 위치입니다.
	 * 0~19 : 1월~10월
	 * 20 : 카드 뒷면(미공개카드)
	 */
	public final static String[] default_Shape =
		{"./image/Default_Card/1-1.png", "./image/Default_Card/1-2.png", "./image/Default_Card/2-1.png",
			"./image/Default_Card/2-2.png", "./image/Default_Card/3-1.png", "./image/Default_Card/3-2.png",
			"./image/Default_Card/4-1.png", "./image/Default_Card/4-2.png", "./image/Default_Card/5-1.png",
			"./image/Default_Card/5-2.png", "./image/Default_Card/6-1.png", "./image/Default_Card/6-2.png",
			"./image/Default_Card/7-1.png", "./image/Default_Card/7-2.png", "./image/Default_Card/8-1.png",
			"./image/Default_Card/8-2.png", "./image/Default_Card/9-1.png", "./image/Default_Card/9-2.png",
			"./image/Default_Card/10-1.png", "./image/Default_Card/10-2.png", "./image/backcard.png"};
}
