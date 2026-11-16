package com.preen.preen_backend.accommodation.init;

import java.util.HashMap;
import java.util.Map;

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
 * - 예시 숙소 13건: 화면 확인용. 숙소 테이블이 비어 있을 때만 등록
 * 이미 데이터가 있으면 아무것도 하지 않으므로 서버를 여러 번 켜도 중복되지 않습니다.
 *
 * 코드(HOTEL, SEOUL 등)는 프론트엔드의 categoryBarItems.ts / regionItems.ts 와 같아야 합니다.
 */
@Component
@RequiredArgsConstructor
public class AccommodationDataInitializer implements ApplicationRunner {

	/** { 코드, 화면 이름 } — 적힌 순서가 화면 표시 순서입니다. */
	private static final String[][] CATEGORIES = {
			{ "MOTEL", "모텔" },
			{ "HOTEL", "호텔" },
			{ "PRIVATE_HOUSE", "독채" },
			{ "POOL_VILLA", "풀빌라" },
			{ "EMOTIONAL_STAY", "감성스테이" },
			{ "GUESTHOUSE", "게스트하우스" },
	};

	private static final String[][] REGIONS = {
			{ "SEOUL", "서울" },
			{ "GANGWON", "강원도" },
			{ "DAEGU", "대구" },
			{ "GWANGJU", "광주" },
			{ "JEJU", "제주도" },
	};

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

		if (accommodationRepository.count() == 0) {
			Map<String, Category> categories = new HashMap<>();
			categoryRepository.findAll().forEach(category -> categories.put(category.getCode(), category));
			Map<String, Region> regions = new HashMap<>();
			regionRepository.findAll().forEach(region -> regions.put(region.getCode(), region));

			SampleSaver sample = new SampleSaver(categories, regions);
			// 카테고리 6개 × 지역 5개에 골고루 나눈 예시 숙소 13건
			// (지역별: 서울 3, 강원도 3, 대구 2, 광주 2, 제주도 3)
			sample.save("MOTEL", "SEOUL", "스테이 인 종로", "서울 종로구 종로 120", "/images/generated-3.png", 55000);
			sample.save("MOTEL", "DAEGU", "동성로 루미 모텔", "대구 중구 동성로 45", "/images/generated-3.png", 50000);
			sample.save("HOTEL", "SEOUL", "시온 시티 호텔", "서울 중구 을지로 88", "/images/generated-1.png", 160000);
			sample.save("HOTEL", "GANGWON", "시온 오션 호텔", "강원 강릉시 창해로 17", "/images/generated-1.png", 145000);
			sample.save("HOTEL", "JEJU", "해담 호텔 제주", "제주 서귀포시 중문관광로 72", "/images/generated-1.png", 190000);
			sample.save("PRIVATE_HOUSE", "GANGWON", "솔숲 독채 평창", "강원 평창군 봉평면 솔숲길 21", "/images/generated-2.png", 220000);
			sample.save("PRIVATE_HOUSE", "GWANGJU", "무등 독채", "광주 동구 무등로 310", "/images/generated-2.png", 170000);
			sample.save("POOL_VILLA", "JEJU", "온유 풀빌라 제주", "제주 제주시 애월읍 애월로 55", "/images/generated.png", 320000);
			sample.save("POOL_VILLA", "GANGWON", "숲결 풀빌라 양양", "강원 양양군 현남면 숲길 12", "/images/generated.png", 290000);
			sample.save("EMOTIONAL_STAY", "JEJU", "온유 스테이 제주", "제주 제주시 구좌읍 해맞이해안로 400", "/images/generated.png", 180000);
			sample.save("EMOTIONAL_STAY", "DAEGU", "서가 스테이 대구", "대구 중구 달구벌대로 2100", "/images/generated-2.png", 120000);
			sample.save("GUESTHOUSE", "SEOUL", "여행자의 집 서촌", "서울 종로구 자하문로 40", "/images/generated-3.png", 58000);
			sample.save("GUESTHOUSE", "GWANGJU", "양림 게스트하우스", "광주 남구 양림로 66", "/images/generated-3.png", 45000);
		}
	}

	/** 예시 숙소 한 건을 저장하는 도우미 */
	private class SampleSaver {

		private final Map<String, Category> categories;
		private final Map<String, Region> regions;

		SampleSaver(Map<String, Category> categories, Map<String, Region> regions) {
			this.categories = categories;
			this.regions = regions;
		}

		void save(String categoryCode, String regionCode, String name, String address, String thumbnailUrl,
				int minNightlyPrice) {
			Category category = categories.get(categoryCode);
			Region region = regions.get(regionCode);
			if (category == null || region == null) {
				return; // 해당 코드의 카테고리·지역이 DB에 없으면 건너뜁니다.
			}
			accommodationRepository.save(new Accommodation(category, region, name, address, thumbnailUrl,
					minNightlyPrice, ListingStatus.LISTED));
		}

	}

}
