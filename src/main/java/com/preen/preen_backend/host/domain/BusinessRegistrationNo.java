package com.preen.preen_backend.host.domain;




// 해당 클래스는 어디서나 쓰는 범용 문자열 도구가 아니라, 호스트 자격을 검증하는 국세청 고유의 비즈니스 핵심 규칙(도메인 지식)을 담당합니다.
// 엔티티가 생성될 때 잘못된 번호가 DB에 들어가지 않도록, 도메인 계층 스스로 올바른 데이터(불변식)를 검증하고 보장하기 위해 여기에 둡니다.


import java.util.Optional;

/**
 * 사업자등록번호 형식 검증. 숫자 10자리(하이픈 허용)이고 마지막 자리가 검증 번호와 맞아야 한다.
 * 국세청에 실제 등록된 번호인지는 확인하지 않는다.
 */
public final class BusinessRegistrationNo {
   private static final int[] weights = {1,3,7,1,3,7,1,3,5};

   private BusinessRegistrationNo(){
   }

   public static Optional<String> nomalize(String input){
       if( input == null || !input.matches("\\d{3}-?\\d{2}-?\\d{5}")){
           return Optional.empty();
       }
       String digits = input.replace("-","");
       return checkDigit(digits.substring(0,9))==digits.charAt(9)-'0'
               ? Optional.of(digits)
               : Optional.empty();

   }

   public static int checkDigit(String firstNine){
       int sum=0;
       for(int i =0; i<9; i++){
           sum += (firstNine.charAt(i)-'0')*weights[i];
       }
       sum += (firstNine.charAt(8)-'0')* 5/10;
       return (10-sum%10)%10;
   }


}