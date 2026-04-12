package com.fromm.leafmap.domain.chatbot.service;

import com.fromm.leafmap.domain.member.entity.Member;
import com.fromm.leafmap.domain.member.entity.Role;
import com.fromm.leafmap.domain.member.repository.MemberRepository;
import com.fromm.leafmap.domain.post.document.PostDocument;
import com.fromm.leafmap.domain.post.entity.BoardType;
import com.fromm.leafmap.domain.post.entity.Post;
import com.fromm.leafmap.domain.post.repository.PostRepository;
import com.fromm.leafmap.domain.post.repository.PostSearchRepository;
import com.fromm.leafmap.domain.post.service.PostIndexService;
import com.fromm.leafmap.domain.post.service.PostSearchService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * MySQL LIKE vs ES+Nori 검색 성능 비교 테스트.
 *
 * 데이터를 1,000 → 5,000 → 10,000건으로 늘려가면서
 * 동일한 검색 쿼리의 응답 시간을 비교
 */
@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Tag("integration")
@ActiveProfiles("test")
@DisplayName("검색 성능 비교: MySQL LIKE vs ES+Nori")
class SearchPerformanceTest {

	@Autowired private PostRepository postRepository;
	@Autowired private PostSearchRepository postSearchRepository;
	@Autowired private PostSearchService postSearchService;
	@Autowired private PostIndexService postIndexService;
	@Autowired private MemberRepository memberRepository;

	private Member testMember;
	private final List<Long> savedPostIds = new ArrayList<>();

	// ── 데이터 자동 생성용 소재 ─────────────────────────────────────────────

	private static final String[] RESTAURANT_TITLES = {
		"맛집 추천", "식당 후기", "카페 소개", "밥집 리뷰", "고깃집 모음",
		"분식집 추천", "냉면 전문점", "국밥 맛집", "떡볶이 스팟", "치킨 리뷰",
		"파스타 식당", "라멘 후기", "중국집 탕수육", "버거 비교", "초밥 맛집",
		"피자 맛있는 곳", "브런치 카페", "베이커리 추천", "디저트 모음", "야식 배달"
	};

	private static final String[] FACILITY_TITLES = {
		"도서관 이용 안내", "체육관 예약 방법", "학생식당 메뉴", "열람실 좌석",
		"프린터 출력", "스터디룸 예약", "헬스장 이용", "컴퓨터실 안내",
		"수영장 등록", "세탁실 이용법"
	};

	private static final String[] SHORTCUT_TITLES = {
		"지름길 발견", "빠른 길 안내", "최단 경로", "실내 이동 루트",
		"연결 통로 정리", "계단 지름길", "뒷길 이동법", "우회 경로"
	};

	private static final String[] CAMPUS_TITLES = {
		"수강신청 꿀팁", "장학금 신청", "동아리 추천", "교양 과목 리스트",
		"시험기간 생존법", "복수전공 가이드", "인턴십 지원", "학점 관리 전략"
	};

	private static final String[] ADDRESSES = {
		"성신", "혜화", "동선동", "성북", "보문", null
	};

	private static final String[] CONTENT_FRAGMENTS = {
		"학교 근처에 있어서 접근성이 좋아요.",
		"가격이 저렴하고 양이 많아요.",
		"분위기가 좋고 깔끔해요.",
		"친구들이랑 자주 가는 곳이에요.",
		"혼자 가기에도 부담 없어요.",
		"점심시간에는 사람이 많으니 일찍 가세요.",
		"이용 방법이 간단하고 편리해요.",
		"학생증이 있으면 할인받을 수 있어요.",
		"예약은 포털에서 가능해요.",
		"주말에도 운영하니까 참고하세요.",
		"신입생들에게 특히 추천해요.",
		"선배들 사이에서 입소문 난 곳이에요.",
		"메뉴가 자주 바뀌어서 매번 새로워요.",
		"계절마다 다른 느낌이 있어요.",
		"후문 쪽에 있어서 잘 모르는 사람이 많아요.",
	};

	// ── 성능 측정용 검색 쿼리 ─────────────────────────────────────────────

	private static final String[] SEARCH_QUERIES = {
		"맛집",
		"맛있는 곳",
		"도서관 이용시간",
		"카페 추천",
		"지름길",
		"밥 먹을 데",
		"체육관 예약",
		"수강신청 방법",
		"성신여대 근처 식당",
		"공부할 장소",
	};

	private static final int WARMUP_RUNS = 3;
	private static final int MEASURE_RUNS = 10;

	// ══════════════════════════════════════════════════════════════════════════
	// Setup / Teardown
	// ══════════════════════════════════════════════════════════════════════════

	@BeforeAll
	void setUp() {
		testMember = memberRepository.save(Member.builder()
			.loginId("test-performance")
			.password("test-pw")
			.nickname("성능테스터")
			.studentId("20240002")
			.role(Role.USER)
			.build());
	}

	@AfterAll
	void tearDown() {
		// 데이터 정리
		if (!savedPostIds.isEmpty()) {
			postSearchRepository.deleteAllById(savedPostIds);
			postRepository.deleteAllById(savedPostIds);
		}
		memberRepository.delete(testMember);
		System.out.println("\n[Teardown] 정리 완료");
	}

	// ══════════════════════════════════════════════════════════════════════════
	// 테스트 실행
	// ══════════════════════════════════════════════════════════════════════════

	@Test
	@Order(1)
	@DisplayName("데이터 규모별 성능 비교: 1K → 5K → 10K")
	void performanceByDataSize() {
		int[] sizes = {1_000, 5_000, 10_000};

		System.out.println();
		System.out.println("═".repeat(70));
		System.out.println("              성능 비교: MySQL LIKE vs ES+Nori");
		System.out.println("═".repeat(70));

		List<SizeResult> sizeResults = new ArrayList<>();

		for (int targetSize : sizes) {
			// 이전 데이터 정리
			if (!savedPostIds.isEmpty()) {
				postSearchRepository.deleteAllById(savedPostIds);
				postRepository.deleteAllById(savedPostIds);
				savedPostIds.clear();
			}

			// 데이터 생성
			System.out.printf("%n▶ 데이터 %,d건 생성 중...%n", targetSize);
			generateTestData(targetSize);

			// ES 인덱싱 안정화 대기
			try { Thread.sleep(3000); } catch (InterruptedException ignored) {}

			System.out.printf("  생성 완료. MySQL: %,d건 / ES: %,d건%n",
				savedPostIds.size(), postSearchRepository.count());

			// 성능 측정
			SizeResult result = measurePerformance(targetSize);
			sizeResults.add(result);
		}

		// 종합 리포트
		printPerformanceReport(sizeResults);
	}

	// ══════════════════════════════════════════════════════════════════════════
	// 데이터 자동 생성
	// ══════════════════════════════════════════════════════════════════════════

	private void generateTestData(int count) {
		BoardType[] types = {
			BoardType.RESTAURANT, BoardType.FACILITY_USAGE,
			BoardType.SHORTCUTS, BoardType.CAMPUS_LIFE_TIPS
		};

		// 비율: RESTAURANT 40%, FACILITY 25%, SHORTCUTS 20%, CAMPUS 15%
		int[] ratios = {40, 25, 20, 15};
		String[][] titleSets = {RESTAURANT_TITLES, FACILITY_TITLES, SHORTCUT_TITLES, CAMPUS_TITLES};

		List<Post> batch = new ArrayList<>();
		ThreadLocalRandom rng = ThreadLocalRandom.current();

		int generated = 0;
		for (int t = 0; t < types.length; t++) {
			int typeCount = count * ratios[t] / 100;
			String[] titles = titleSets[t];

			for (int i = 0; i < typeCount && generated < count; i++) {
				String baseTitle = titles[rng.nextInt(titles.length)];
				String address = ADDRESSES[rng.nextInt(ADDRESSES.length)];
				int likeCount = rng.nextInt(0, 150);

				String title = String.format("%s #%d", baseTitle, generated + 1);

				// 내용: 2~3개 문장 조합
				StringBuilder content = new StringBuilder();
				int fragCount = rng.nextInt(2, 4);
				for (int f = 0; f < fragCount; f++) {
					content.append(CONTENT_FRAGMENTS[rng.nextInt(CONTENT_FRAGMENTS.length)]);
					content.append(" ");
				}

				batch.add(Post.builder()
					.title(title)
					.content(content.toString().trim())
					.address(address)
					.boardType(types[t])
					.isPublic(true)
					.likeCount(likeCount)
					.badge(likeCount >= 10)
					.member(testMember)
					.build());

				generated++;
			}
		}

		// 배치 저장
		List<Post> saved = postRepository.saveAll(batch);
		saved.forEach(p -> savedPostIds.add(p.getId()));

		// ES 벌크 인덱싱
		List<PostDocument> docs = saved.stream()
			.map(postIndexService::toDocument)
			.toList();
		postSearchRepository.saveAll(docs);
	}

	// ══════════════════════════════════════════════════════════════════════════
	// 성능 측정
	// ══════════════════════════════════════════════════════════════════════════

	record QueryResult(String query, double mysqlAvgMs, double esAvgMs, int mysqlCount, int esCount) {}
	record SizeResult(int dataSize, List<QueryResult> queryResults, double mysqlOverallAvg, double esOverallAvg) {}

	private SizeResult measurePerformance(int dataSize) {
		System.out.printf("%n  ┌─ 성능 측정 (%,d건) ─────────────────────────────────┐%n", dataSize);
		System.out.printf("  │ %-20s │ %8s │ %8s │ 비율   │%n", "쿼리", "MySQL", "ES", "");
		System.out.printf("  ├─────────────────────┼──────────┼──────────┼────────┤%n");

		List<QueryResult> queryResults = new ArrayList<>();

		for (String query : SEARCH_QUERIES) {
			for (int i = 0; i < WARMUP_RUNS; i++) {
				postRepository.searchPostsByCondition(null, null, null, query);
				postSearchService.search(null, null, null, query);
			}

			// MySQL 측정
			long mysqlTotal = 0;
			int mysqlCount = 0;
			for (int i = 0; i < MEASURE_RUNS; i++) {
				long start = System.nanoTime();
				List<Post> results = postRepository.searchPostsByCondition(null, null, null, query);
				mysqlTotal += System.nanoTime() - start;
				mysqlCount = results.size();
			}
			double mysqlAvgMs = (double) mysqlTotal / MEASURE_RUNS / 1_000_000;

			// ES 측정
			long esTotal = 0;
			int esCount = 0;
			for (int i = 0; i < MEASURE_RUNS; i++) {
				long start = System.nanoTime();
				List<PostDocument> results = postSearchService.search(null, null, null, query);
				esTotal += System.nanoTime() - start;
				esCount = results.size();
			}
			double esAvgMs = (double) esTotal / MEASURE_RUNS / 1_000_000;

			double ratio = mysqlAvgMs > 0 ? esAvgMs / mysqlAvgMs : 0;
			String ratioStr = ratio < 1
				? String.format("%.1fx빠름", 1.0 / ratio)
				: String.format("%.1fx느림", ratio);

			// 쿼리가 길면 자르기
			String displayQuery = query.length() > 18
				? query.substring(0, 18) + ".."
				: query;

			System.out.printf("  │ %-20s │ %6.1fms │ %6.1fms │ %s │%n",
				displayQuery, mysqlAvgMs, esAvgMs, ratioStr);

			queryResults.add(new QueryResult(query, mysqlAvgMs, esAvgMs, mysqlCount, esCount));
		}

		double mysqlOverall = queryResults.stream().mapToDouble(QueryResult::mysqlAvgMs).average().orElse(0);
		double esOverall = queryResults.stream().mapToDouble(QueryResult::esAvgMs).average().orElse(0);

		System.out.printf("  ├─────────────────────┼──────────┼──────────┼────────┤%n");
		System.out.printf("  │ %-20s │ %6.1fms │ %6.1fms │        │%n", "평균", mysqlOverall, esOverall);
		System.out.printf("  └─────────────────────┴──────────┴──────────┴────────┘%n");

		return new SizeResult(dataSize, queryResults, mysqlOverall, esOverall);
	}

	// ══════════════════════════════════════════════════════════════════════════
	// 종합 리포트
	// ══════════════════════════════════════════════════════════════════════════

	private void printPerformanceReport(List<SizeResult> results) {
		System.out.println();
		System.out.println("═".repeat(70));
		System.out.println("                    성능 비교 종합 리포트");
		System.out.println("═".repeat(70));

		System.out.printf("%n  %-12s │ %10s │ %10s │ %s%n",
			"데이터 규모", "MySQL 평균", "ES 평균", "ES 속도 비교");
		System.out.println("  " + "─".repeat(55));

		for (SizeResult r : results) {
			double ratio = r.mysqlOverallAvg() > 0
				? r.mysqlOverallAvg() / r.esOverallAvg()
				: 0;
			String comparison = ratio > 1
				? String.format("%.1fx 빠름 ✅", ratio)
				: String.format("%.1fx 느림", 1.0 / ratio);

			System.out.printf("  %,10d건 │ %8.1fms │ %8.1fms │ %s%n",
				r.dataSize(), r.mysqlOverallAvg(), r.esOverallAvg(), comparison);
		}

		System.out.println("═".repeat(70));
	}
}