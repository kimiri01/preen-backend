package com.preen.preen_backend.accommodation.init;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.preen.preen_backend.accommodation.domain.Accommodation;
import com.preen.preen_backend.accommodation.domain.Category;
import com.preen.preen_backend.accommodation.domain.ListingStatus;
import com.preen.preen_backend.accommodation.domain.Region;
import com.preen.preen_backend.accommodation.repository.AccommodationRepository;
import com.preen.preen_backend.accommodation.repository.CategoryRepository;
import com.preen.preen_backend.accommodation.repository.RegionRepository;

import lombok.RequiredArgsConstructor;

/**
 * 서버가 시작될 때 한 번 실행되어 초기 데이터를 넣습니다.
 * - 카테고리 6개, 지역 5개: 각 테이블이 비어 있을 때만 등록
 * - 예시 숙소 36건(카테고리별 6건): 숙소 테이블이 비어 있을 때만 등록
 * 이미 데이터가 있으면 아무것도 하지 않으므로 서버를 여러 번 켜도 중복되지 않습니다.
 *
 * 코드(HOTEL, SEOUL 등)는 프론트엔드의 categoryBarItems.ts / regionItems.ts 와 같아야 합니다.
 */
@Component
@RequiredArgsConstructor
public class AccommodationDataInitializer implements ApplicationRunner {

	/** { 코드, 화면 이름, 이미지 파일 이름 앞부분 } — 적힌 순서가 화면 표시 순서입니다. */
	private static final String[][] CATEGORIES = {
			{ "MOTEL", "모텔", "motel" },
			{ "HOTEL", "호텔", "hotel" },
			{ "PRIVATE_HOUSE", "독채", "private-house" },
			{ "POOL_VILLA", "풀빌라", "pool-villa" },
			{ "EMOTIONAL_STAY", "감성스테이", "emotional-stay" },
			{ "GUESTHOUSE", "게스트하우스", "guesthouse" },
	};

	/** { 코드, 화면 이름, 숙소 이름 뒤에 붙일 짧은 지역명 } */
	private static final String[][] REGIONS = {
			{ "SEOUL", "서울", "서울" },
			{ "GANGWON", "강원도", "강원" },
			{ "DAEGU", "대구", "대구" },
			{ "GWANGJU", "광주", "광주" },
			{ "JEJU", "제주도", "제주" },
	};

	/** 카테고리별 예시 숙소 이름 6개 (CATEGORIES와 같은 순서) */
	private static final String[][] SAMPLE_NAMES = {
			{ "루나 모텔", "모먼트 모텔", "블루문 모텔", "어반 모텔", "라온 모텔", "시그니처 모텔" },
			{ "시온 시티 호텔", "해담 호텔", "라벤더 호텔", "더 스카이 호텔", "오로라 호텔", "메리안 호텔" },
			{ "솔숲 독채", "돌담 독채", "하늘채 독채", "고요 독채", "느린별 독채", "마루 독채" },
			{ "온유 풀빌라", "숲결 풀빌라", "아쿠아 풀빌라", "윤슬 풀빌라", "파란 풀빌라", "썸머 풀빌라" },
			{ "온유 스테이", "서가 스테이", "오후 스테이", "결 스테이", "머묾 스테이", "포근 스테이" },
			{ "여행자의 집", "양림 게스트하우스", "바람 게스트하우스", "골목 게스트하우스", "별빛 게스트하우스", "쉼표 게스트하우스" },
	};

	/** 지역별 도로명 (REGIONS와 같은 순서). 뒤에 건물 번호를 랜덤으로 붙여 주소를 만듭니다. */
	private static final String[][] STREETS = {
			{ "서울 종로구 종로", "서울 마포구 와우산로", "서울 중구 을지로", "서울 강남구 테헤란로", "서울 용산구 이태원로" },
			{ "강원 강릉시 창해로", "강원 속초시 해오름로", "강원 평창군 봉평면 솔숲길", "강원 양양군 현남면 해변길", "강원 춘천시 호반로" },
			{ "대구 중구 동성로", "대구 수성구 들안로", "대구 동구 팔공산로", "대구 중구 달구벌대로" },
			{ "광주 동구 무등로", "광주 남구 양림로", "광주 서구 상무대로", "광주 북구 첨단과기로" },
			{ "제주 제주시 애월읍 애월로", "제주 서귀포시 중문관광로", "제주 제주시 구좌읍 해맞이해안로", "제주 서귀포시 성산읍 일출로" },
	};

	/** 카테고리마다 만들 예시 숙소 수 */
	private static final int SAMPLES_PER_CATEGORY = 6;

	/** 1박 요금 범위(원). 이 범위 안에서 1,000원 단위로 랜덤하게 정합니다. */
	private static final int MIN_PRICE = 50_000;
	private static final int MAX_PRICE = 150_000;

	/**
	 * 랜덤의 "시작 번호(seed)". 같은 번호로 시작하면 매번 같은 순서로 랜덤 값이 나옵니다.
	 * 덕분에 팀원 누가 서버를 켜도 똑같은 예시 데이터(지역·가격)가 만들어집니다.
	 */
	private static final long RANDOM_SEED = 20261117L;

	/** 이전 버전(13건짜리)의 예시 숙소가 쓰던 이미지 경로. 이 경로를 쓰는 숙소는 새 예시로 교체합니다. */
	private static final String OLD_SAMPLE_IMAGE_PREFIX = "/images/generated";

	/** 잠깐 썼던 일러스트(.svg) 이미지. 이 이미지를 쓰는 숙소도 실제 사진(.jpg) 예시로 교체합니다. */
	private static final String OLD_ILLUSTRATION_SUFFIX = ".svg";

	private final CategoryRepository categoryRepository;
	private final RegionRepository regionRepository;
	private final AccommodationRepository accommodationRepository;

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		if (categoryRepository.count() == 0) {
			for (int i = 0; i < CATEGORIES.length; i++) {
				categoryRepository.save(new Category(CATEGORIES[i][0], CATEGORIES[i][1], i + 1));
			}
		}

		if (regionRepository.count() == 0) {
			for (int i = 0; i < REGIONS.length; i++) {
				regionRepository.save(new Region(REGIONS[i][0], REGIONS[i][1], i + 1));
			}
		}

		// 어제 넣은 예시 숙소 13건(같은 이미지를 여러 숙소가 같이 씀)이 남아 있으면 지우고 새 예시로 바꿉니다.
		accommodationRepository.deleteByThumbnailUrlStartingWith(OLD_SAMPLE_IMAGE_PREFIX);
		// 일러스트(.svg) 이미지로 들어간 예시 숙소가 있으면 지우고 실제 사진 예시로 다시 넣습니다.
		accommodationRepository.deleteByThumbnailUrlEndingWith(OLD_ILLUSTRATION_SUFFIX);

		if (accommodationRepository.count() == 0) {
			saveSampleAccommodations();
		}
	}

	/**
	 * 카테고리 6개 × 6건 = 36건의 예시 숙소를 저장합니다.
	 * - 이미지: 숙소마다 다른 파일 (/images/stays/{카테고리}-{번호}.jpg, 프론트 public 폴더에 있음)
	 * - 지역: 5개 지역 중 랜덤
	 * - 1박 요금: 50,000원 ~ 150,000원 사이 랜덤 (1,000원 단위)
	 */
	private void saveSampleAccommodations() {
		Map<String, Category> categories = new HashMap<>();
		categoryRepository.findAll().forEach(category -> categories.put(category.getCode(), category));
		Map<String, Region> regions = new HashMap<>();
		regionRepository.findAll().forEach(region -> regions.put(region.getCode(), region));

		Random random = new Random(RANDOM_SEED);
		int priceSteps = (MAX_PRICE - MIN_PRICE) / 1_000; // 100단계 → 50,000 ~ 150,000

		for (int c = 0; c < CATEGORIES.length; c++) {
			Category category = categories.get(CATEGORIES[c][0]);
			if (category == null) {
				continue; // 해당 코드의 카테고리가 DB에 없으면 건너뜁니다.
			}
			for (int n = 0; n < SAMPLES_PER_CATEGORY; n++) {
				int regionIndex = random.nextInt(REGIONS.length);
				Region region = regions.get(REGIONS[regionIndex][0]);
				if (region == null) {
					continue;
				}
				String[] streets = STREETS[regionIndex];
				String address = streets[random.nextInt(streets.length)] + " " + (10 + random.nextInt(390));
				int price = MIN_PRICE + random.nextInt(priceSteps + 1) * 1_000;

				String name = SAMPLE_NAMES[c][n] + " " + REGIONS[regionIndex][2];
				String thumbnailUrl = "/images/stays/" + CATEGORIES[c][2] + "-" + (n + 1) + ".jpg";

				accommodationRepository.save(new Accommodation(category, region, name, address, thumbnailUrl, price,
						ListingStatus.LISTED));
			}
		}
	}

}
