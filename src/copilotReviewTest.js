// 총 숙박 요금을 계산하는 테스트용 클래스
public class CopilotReviewTest {

    // nights: 숙박일 수, pricePerNight: 1박 가격(원)
    // discountRate: 할인율. 0~100 사이의 퍼센트 값으로 들어온다 (예: 10 → 10% 할인)
    public static long calcTotalPrice(int nights, long pricePerNight, int discountRate) {
        // 할인 전 금액 = 숙박일 수 × 1박 가격
        long total = nights * pricePerNight;
        // 할인 전 금액에서 할인 금액을 빼서 최종 금액을 돌려준다
        return total - total * discountRate;
    }
}

// src/test/copilotReviewTest.js
export function getUser(id) {
    const users = JSON.parse(localStorage.getItem("users"));
    return users.find(u => u.id == id).name;
}