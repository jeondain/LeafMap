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
import java.util.stream.Collectors;

/**
 * MySQL LIKE vs ES+Nori 검색 정확도 비교 테스트
 *
 * 설계 원칙:
 * 1. 게시글에는 직접 키워드만 사용 (우회 표현 배제)
 * 2. 질문은 우회/활용형/오타로 검색
 * 3. MySQL이 OR 분리 검색을 해도 못 찾는 케이스를 설계
 *    → "공부할 장소": "공부할"도 "장소"도 게시글에 없으면 MySQL 0건
 * 4. 한글 오타는 자모 단위 변경(카폐→카페)이 안 되므로,
 *    초성/받침 변경 수준의 오타만 테스트
 */
@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Tag("integration")
@ActiveProfiles("test")
@DisplayName("검색 정확도 비교: MySQL LIKE vs ES+Nori")
class SearchAccuracyTest {

	@Autowired private PostRepository postRepository;
	@Autowired private PostSearchRepository postSearchRepository;
	@Autowired private PostSearchService postSearchService;
	@Autowired private PostIndexService postIndexService;
	@Autowired private MemberRepository memberRepository;

	private Member testMember;
	private final List<Long> savedPostIds = new ArrayList<>();
	private final List<ComparisonResult> allResults = new ArrayList<>();

	// ══════════════════════════════════════════════════════════════════════════
	// 테스트 데이터
	// ══════════════════════════════════════════════════════════════════════════

	private static final Object[][] TEST_POSTS = {

		// ── RESTAURANT (10개) ──
		{"성신여대 정문 앞 김치찌개 맛집",
			"정문 앞 골목 맛집. 김치찌개 8,000원 깍두기 무한리필.",
			"성신", BoardType.RESTAURANT},
		{"혜화역 근처 파스타 식당",
			"혜화역 3번 출구 도보 5분. 런치 파스타 9,000원.",
			"혜화", BoardType.RESTAURANT},
		{"학교 앞 3000원 김밥 분식집",
			"후문 근처 분식집. 참치김밥 양 많고 가격 저렴.",
			"성신", BoardType.RESTAURANT},
		{"동선동 카페 아메리카노 4500원",
			"동선동 골목 카페. 아메리카노 저렴하고 조용한 분위기.",
			"동선동", BoardType.RESTAURANT},
		{"혼자 식사 가능한 식당 모음",
			"1인 메뉴 식당들 정리. 바 형식 좌석 편리.",
			null, BoardType.RESTAURANT},
		{"가성비 점심 맛집 정리",
			"5,000원 이하 저렴한 맛집 모음.",
			"성신", BoardType.RESTAURANT},
		{"성신여대 카페 투어 코스",
			"성신 근처 카페 코스 정리. 아메리카노 추천.",
			"성신", BoardType.RESTAURANT},
		{"혜화 감성 카페 디저트 맛집",
			"혜화 분위기 카페 모음. 디저트 판매.",
			"혜화", BoardType.RESTAURANT},
		{"성북구 삼겹살 갈비 식당",
			"성북구 고기 식당 세 곳 비교. 가격 합리적.",
			"성북", BoardType.RESTAURANT},
		{"야식 치킨 피자 배달 맛집",
			"새벽 배달 치킨 피자 맛집 정리.",
			null, BoardType.RESTAURANT},

		// ── FACILITY_USAGE (8개) ──
		{"중앙도서관 층별 좌석 안내",
			"1층 그룹 스터디, 2층 개인 열람실, 3층 노트북 존. 운영 9시~22시.",
			null, BoardType.FACILITY_USAGE},
		{"체육관 예약 안내",
			"체육관 학교 포털 예약. 3일 전부터 가능. 2시간 단위.",
			null, BoardType.FACILITY_USAGE},
		{"학생식당 메뉴 가격표",
			"학생식당 A코너 3,500원, B코너 4,000원. 월~금 11시~14시 운영.",
			null, BoardType.FACILITY_USAGE},
		{"열람실 좌석 배정 안내",
			"열람실 오전 7시부터 개방. 포털 좌석 예약 가능.",
			null, BoardType.FACILITY_USAGE},
		{"학교 헬스장 등록 안내",
			"헬스장 한 학기 등록비 50,000원. 등록 후 자유 이용.",
			null, BoardType.FACILITY_USAGE},
		{"도서관 야간 운영 안내",
			"도서관 야간 22시까지. 시험기간 열람실 24시간 개방.",
			null, BoardType.FACILITY_USAGE},
		{"체육관 주말 운영 안내",
			"체육관 주말 9시~18시 운영. 공휴일 단축 운영.",
			null, BoardType.FACILITY_USAGE},
		{"프린터 무료 출력 안내",
			"도서관 1층 프린터. 학생 계정 월 100장 무료 출력.",
			null, BoardType.FACILITY_USAGE},

		// ── SHORTCUTS (6개) ──
		{"혜인관에서 도서관 3분컷 지름길",
			"혜인관 뒷문 계단 이용 3분 도서관 도착.",
			null, BoardType.SHORTCUTS},
		{"정문에서 운동장 최단 경로",
			"정문 왼쪽 직진 운동장 도착. 지름길.",
			null, BoardType.SHORTCUTS},
		{"비 올 때 실내 이동 경로",
			"본관-연결통로-학생회관-지하통로 순서 이동.",
			null, BoardType.SHORTCUTS},
		{"뒷문에서 학생회관 최단 경로",
			"뒷문 오른쪽 학생회관 2분. 지름길.",
			null, BoardType.SHORTCUTS},
		{"강의동 간 최단 이동 경로",
			"건물 간 지하 통로 이용 최단 경로.",
			null, BoardType.SHORTCUTS},
		{"야간 안전 이동 경로",
			"야간 조명 있는 안전 경로 안내.",
			null, BoardType.SHORTCUTS},

		// ── CAMPUS_LIFE_TIPS (6개) ──
		{"수강신청 전략 정리",
			"수강신청 과목 코드 미리 복사. 서버 열리자마자 클릭.",
			null, BoardType.CAMPUS_LIFE_TIPS},
		{"장학금 신청 기간 안내",
			"국가장학금 매 학기 초 신청. 기간 놓치면 수령 불가.",
			null, BoardType.CAMPUS_LIFE_TIPS},
		{"신입생 동아리 추천 5선",
			"신입생 가입 추천 동아리 다섯 곳 정리.",
			null, BoardType.CAMPUS_LIFE_TIPS},
		{"교양 과목 추천 리스트",
			"학점 잘 나오는 교양 과목 모음. 추천 순위 정리.",
			null, BoardType.CAMPUS_LIFE_TIPS},
		{"복수전공 신청 안내",
			"복수전공 2학년 1학기부터 신청 가능. 지원 조건 확인.",
			null, BoardType.CAMPUS_LIFE_TIPS},
		{"시험기간 학습 전략",
			"시험 2주 전부터 준비. 노트 정리 기출문제 풀이 핵심.",
			null, BoardType.CAMPUS_LIFE_TIPS},
	};

	// ══════════════════════════════════════════════════════════════════════════
	// 테스트 케이스
	// ══════════════════════════════════════════════════════════════════════════

	record TestCase(String query, List<String> expectedKeywords, String category) {}

	record ComparisonResult(
		TestCase testCase, boolean mysqlHit, boolean esHit,
		int mysqlCount, int esCount, long mysqlMs, long esMs
	) {}

	private final List<TestCase> testCases = List.of(

		// ━━━ 카테고리 1: 정확한 키워드 (5개) ━━━
		new TestCase("맛집",     List.of("맛집"),     "정확한 키워드"),
		new TestCase("도서관",   List.of("도서관"),   "정확한 키워드"),
		new TestCase("지름길",   List.of("지름길"),   "정확한 키워드"),
		new TestCase("수강신청", List.of("수강신청"), "정확한 키워드"),
		new TestCase("카페",     List.of("카페"),     "정확한 키워드"),

		// ━━━ 카테고리 2: 유사 표현 (7개) ━━━
		new TestCase("맛있는 곳",      List.of("맛집", "식당"),       "유사 표현"),
		new TestCase("밥 먹을 데",     List.of("맛집", "식당"),       "유사 표현"),
		new TestCase("커피 한잔",      List.of("카페", "아메리카노"), "유사 표현"),
		new TestCase("운동할 데",      List.of("체육관", "헬스"),     "유사 표현"),
		new TestCase("공부할 데",      List.of("열람실", "도서관"),   "유사 표현"),
		new TestCase("빨리 가는 길",   List.of("지름길", "최단"),     "유사 표현"),
		new TestCase("장학금 타는 법", List.of("장학금"),             "유사 표현"),

		// ━━━ 카테고리 3: 활용형 변화 (6개) ━━━
		// 게시글에 원형("예약", "신청", "이용")만 있고,
		// 질문은 활용형("예약하고 싶은데", "신청하려고요")으로 검색
		new TestCase("예약하고 싶은데",   List.of("예약"),   "활용형 변화"),
		new TestCase("신청하려고요",      List.of("신청"),   "활용형 변화"),
		new TestCase("이용하려면",        List.of("이용"),   "활용형 변화"),
		new TestCase("등록하고 싶어요",   List.of("등록"),   "활용형 변화"),
		new TestCase("운영하나요",        List.of("운영"),   "활용형 변화"),
		new TestCase("개방하나요",        List.of("개방"),   "활용형 변화"),

		// ━━━ 카테고리 4: 복합 조건 (5개) ━━━
		new TestCase("성신여대 근처 저렴한 밥집",   List.of("성신", "맛집", "식당", "저렴"), "복합 조건"),
		new TestCase("도서관 야간에 이용 가능해?",  List.of("도서관", "야간"),               "복합 조건"),
		new TestCase("혜인관에서 도서관 어떻게 가",  List.of("혜인관", "도서관"),             "복합 조건"),
		new TestCase("혼자 식사할 데",               List.of("혼자", "식당", "식사"),         "복합 조건"),
		new TestCase("체육관 주말에 열어?",          List.of("체육관", "주말"),               "복합 조건"),

		// ━━━ 카테고리 5: 오타 (3개) ━━━
		new TestCase("맛짐",     List.of("맛집"),     "오타"),
		new TestCase("도서괸",   List.of("도서관"),   "오타"),
		new TestCase("수강신쳥", List.of("수강신청"), "오타"),

		// ━━━ 카테고리 6: 검색 불가 (3개) ━━━
		new TestCase("xyzabc123",           List.of(), "검색 불가"),
		new TestCase("화성에서 감자 키우기", List.of(), "검색 불가"),
		new TestCase("양자역학 수업",        List.of(), "검색 불가")
	);

	// ══════════════════════════════════════════════════════════════════════════
	// Setup / Teardown
	// ══════════════════════════════════════════════════════════════════════════

	@BeforeAll
	void setUp() {
		testMember = memberRepository.save(Member.builder()
			.loginId("test-accuracy-v3")
			.password("test-pw")
			.nickname("정확도테스터v3")
			.studentId("20240004")
			.role(Role.USER)
			.build());

		for (int i = 0; i < TEST_POSTS.length; i++) {
			Object[] row = TEST_POSTS[i];
			int likeCount = (i * 7) % 101;

			Post post = Post.builder()
				.title((String) row[0])
				.content((String) row[1])
				.address((String) row[2])
				.boardType((BoardType) row[3])
				.isPublic(true)
				.likeCount(likeCount)
				.badge(likeCount >= 10)
				.member(testMember)
				.build();

			Post saved = postRepository.save(post);
			savedPostIds.add(saved.getId());
			postSearchRepository.save(postIndexService.toDocument(saved));
		}

		try { Thread.sleep(2000); } catch (InterruptedException ignored) {}

		System.out.printf("%n[Setup] 테스트 게시글 %d개 삽입 완료 (MySQL + ES)%n", savedPostIds.size());
		System.out.println("═".repeat(70));
	}

	@AfterAll
	void tearDown() {
		printFinalReport();
		postSearchRepository.deleteAllById(savedPostIds);
		postRepository.deleteAllById(savedPostIds);
		memberRepository.delete(testMember);
		System.out.println("[Teardown] 정리 완료");
	}

	// ══════════════════════════════════════════════════════════════════════════
	// 테스트 실행
	// ══════════════════════════════════════════════════════════════════════════

	@Test
	@DisplayName("전체 정확도 비교")
	void runAllAccuracyTests() {
		String currentCategory = "";

		for (TestCase tc : testCases) {
			if (!tc.category().equals(currentCategory)) {
				currentCategory = tc.category();
				System.out.printf("%n━━━━━━ %s ━━━━━━%n", currentCategory);
			}

			ComparisonResult result = runSingleComparison(tc);
			allResults.add(result);
			printResult(result);
		}
	}

	private ComparisonResult runSingleComparison(TestCase tc) {
		boolean isNoResultCase = tc.expectedKeywords().isEmpty();

		// MySQL
		long t0 = System.currentTimeMillis();
		List<Post> mysqlResults = postRepository.searchPostsByCondition(
			null, null, null, tc.query());
		long mysqlMs = System.currentTimeMillis() - t0;

		boolean mysqlHit = isNoResultCase
			? mysqlResults.isEmpty()
			: mysqlResults.stream().anyMatch(p ->
			tc.expectedKeywords().stream().anyMatch(kw ->
				p.getTitle().contains(kw) || p.getContent().contains(kw)));

		// ES
		long t1 = System.currentTimeMillis();
		List<PostDocument> esResults = postSearchService.search(
			null, null, null, tc.query());
		long esMs = System.currentTimeMillis() - t1;

		boolean esHit = isNoResultCase
			? esResults.isEmpty()
			: esResults.stream().anyMatch(doc ->
			tc.expectedKeywords().stream().anyMatch(kw ->
				doc.getTitle().contains(kw) || doc.getContent().contains(kw)));

		return new ComparisonResult(tc, mysqlHit, esHit,
			mysqlResults.size(), esResults.size(), mysqlMs, esMs);
	}

	// ══════════════════════════════════════════════════════════════════════════
	// 출력
	// ══════════════════════════════════════════════════════════════════════════

	private void printResult(ComparisonResult r) {
		String expected = r.testCase().expectedKeywords().isEmpty()
			? "[0건이어야 정상]"
			: r.testCase().expectedKeywords().toString();

		System.out.printf("질문: \"%s\" | 기대: %s%n", r.testCase().query(), expected);
		System.out.printf("  MySQL: %2d건 | 정확: %s | %dms%n",
			r.mysqlCount(), r.mysqlHit() ? "✅" : "❌", r.mysqlMs());
		System.out.printf("  ES:    %2d건 | 정확: %s | %dms%n%n",
			r.esCount(), r.esHit() ? "✅" : "❌", r.esMs());
	}

	private void printFinalReport() {
		System.out.println();
		System.out.println("═".repeat(70));
		System.out.println("                    정확도 비교 종합 리포트");
		System.out.println("═".repeat(70));

		long mHit = allResults.stream().filter(ComparisonResult::mysqlHit).count();
		long eHit = allResults.stream().filter(ComparisonResult::esHit).count();
		int total = allResults.size();

		System.out.printf("%n총 %d개 질문%n%n", total);
		System.out.println("[ 전체 정확도 ]");
		System.out.printf("  MySQL: %d/%d (%.1f%%)%n", mHit, total, pct(mHit, total));
		System.out.printf("  ES:    %d/%d (%.1f%%)%n%n", eHit, total, pct(eHit, total));

		System.out.println("[ 카테고리별 ]");
		allResults.stream()
			.collect(Collectors.groupingBy(r -> r.testCase().category(),
				LinkedHashMap::new, Collectors.toList()))
			.forEach((cat, results) -> {
				long m = results.stream().filter(ComparisonResult::mysqlHit).count();
				long e = results.stream().filter(ComparisonResult::esHit).count();
				System.out.printf("  %-12s — MySQL: %d/%d | ES: %d/%d%n",
					cat, m, results.size(), e, results.size());
			});

		List<ComparisonResult> esOnly = allResults.stream()
			.filter(r -> r.esHit() && !r.mysqlHit()).toList();
		if (!esOnly.isEmpty()) {
			System.out.printf("%n[ ES만 정확, MySQL은 실패: %d개 ]%n", esOnly.size());
			esOnly.forEach(r -> System.out.printf("  • \"%s\" — ES %d건 vs MySQL %d건%n",
				r.testCase().query(), r.esCount(), r.mysqlCount()));
		}

		System.out.println("═".repeat(70));
	}

	private double pct(long hit, int total) {
		return total > 0 ? (double) hit / total * 100 : 0;
	}
}