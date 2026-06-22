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
import java.util.stream.IntStream;

/**
 * MySQL LIKE vs ES+Nori 검색 정확도 비교 테스트
 *
 * 측정 지표:
 * - Hit@3      : top-3 결과 안에 관련 문서가 있는가 (RAG top-k 기준)
 * - MRR        : 첫 번째 관련 문서의 순위 역수 평균 (순위 품질)
 * - Precision@5: 상위 5개 중 관련 문서 비율 (노이즈 측정)
 *
 * 설계 원칙:
 * 1. 게시글에는 원형/직접 키워드만 사용
 * 2. 질문은 우회 표현·활용형 변화·복합 조건으로 검색
 * 3. MySQL LIKE가 형태소 분석 없이 무너지는 케이스를 집중 설계
 * 4. 오타 내성은 현재 구현 범위 밖이므로 측정 제외
 *    (fuzzy 쿼리 추가 시 별도 테스트로 분리)
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

	// ── 히트 판정 기준 ──────────────────────────────────────────────────────

	/** RAG 파이프라인이 GPT에 넘기는 컨텍스트 크기와 동일하게 맞춤 */
	private static final int HIT_K = 3;
	private static final int PRECISION_K = 5;

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

	/**
	 * @param query            실제 검색 쿼리
	 * @param expectedKeywords 결과 게시글 제목/내용에 포함되어야 할 키워드 (OR 조건)
	 * @param category         테스트 분류
	 */
	record TestCase(String query, List<String> expectedKeywords, String category) {
		boolean isNoResultCase() { return expectedKeywords.isEmpty(); }
	}

	/**
	 * @param testCase      원본 테스트 케이스
	 * @param mysqlHitAt3   MySQL top-3 안에 관련 문서 존재 여부
	 * @param esHitAt3      ES    top-3 안에 관련 문서 존재 여부
	 * @param mysqlMrr      MySQL MRR (첫 관련 문서 순위 역수, 없으면 0)
	 * @param esMrr         ES    MRR
	 * @param mysqlPrecAt5  MySQL Precision@5
	 * @param esPrecAt5     ES    Precision@5
	 * @param mysqlCount    MySQL 전체 결과 수
	 * @param esCount       ES    전체 결과 수
	 * @param mysqlMs       MySQL 응답 시간(ms)
	 * @param esMs          ES    응답 시간(ms)
	 */
	record ComparisonResult(
		TestCase testCase,
		boolean mysqlHitAt3, boolean esHitAt3,
		double mysqlMrr, double esMrr,
		double mysqlPrecAt5, double esPrecAt5,
		int mysqlCount, int esCount,
		long mysqlMs, long esMs
	) {}

	private final List<TestCase> testCases = List.of(

		// ━━━ 카테고리 1: 정확한 키워드 (5개) ━━━
		// MySQL LIKE도 잡을 수 있는 베이스라인
		new TestCase("맛집",     List.of("맛집"),     "정확한 키워드"),
		new TestCase("도서관",   List.of("도서관"),   "정확한 키워드"),
		new TestCase("지름길",   List.of("지름길"),   "정확한 키워드"),
		new TestCase("수강신청", List.of("수강신청"), "정확한 키워드"),
		new TestCase("카페",     List.of("카페"),     "정확한 키워드"),

		// ━━━ 카테고리 2: 유사 표현 (7개) ━━━
		// 게시글에 없는 단어로 검색
		new TestCase("맛있는 곳",      List.of("맛집", "식당"),       "유사 표현"),
		new TestCase("밥 먹을 데",     List.of("맛집", "식당"),       "유사 표현"),
		new TestCase("커피 한잔",      List.of("카페", "아메리카노"), "유사 표현"),
		new TestCase("운동할 데",      List.of("체육관", "헬스"),     "유사 표현"),
		new TestCase("공부할 데",      List.of("열람실", "도서관"),   "유사 표현"),
		new TestCase("빨리 가는 길",   List.of("지름길", "최단"),     "유사 표현"),
		new TestCase("장학금 타는 법", List.of("장학금"),             "유사 표현"),

		// ━━━ 카테고리 3: 활용형 변화 (6개) ━━━
		// 게시글에는 원형("예약", "신청")만 있고, 질문은 활용형으로 검색
		// Nori가 어간을 분리해 매칭하는지 검증하는 핵심 케이스
		new TestCase("예약하고 싶은데", List.of("예약"), "활용형 변화"),
		new TestCase("신청하려고요",    List.of("신청"), "활용형 변화"),
		new TestCase("이용하려면",      List.of("이용"), "활용형 변화"),
		new TestCase("등록하고 싶어요", List.of("등록"), "활용형 변화"),
		new TestCase("운영하나요",      List.of("운영"), "활용형 변화"),
		new TestCase("개방하나요",      List.of("개방"), "활용형 변화"),

		// ━━━ 카테고리 4: 복합 조건 (5개) ━━━
		// 여러 조건이 섞인 자연어 질문 → 형태소 분석 + 멀티 필드 매칭
		new TestCase("성신여대 근처 저렴한 밥집",  List.of("성신", "맛집", "식당", "저렴"), "복합 조건"),
		new TestCase("도서관 야간에 이용 가능해?", List.of("도서관", "야간"),               "복합 조건"),
		new TestCase("혜인관에서 도서관 어떻게 가", List.of("혜인관", "도서관"),            "복합 조건"),
		new TestCase("혼자 식사할 데",             List.of("혼자", "식당", "식사"),          "복합 조건"),
		new TestCase("체육관 주말에 열어?",        List.of("체육관", "주말"),               "복합 조건"),

		// ━━━ 카테고리 5: 검색 불가 (3개) ━━━
		// 양쪽 모두 0건이어야 정상 (특이도 검증)
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
			.loginId("test-accuracy-v4")
			.password("test-pw")
			.nickname("정확도테스터v4")
			.studentId("20240005")
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
		System.out.printf("        판정 기준: Hit@%d / MRR / Precision@%d%n", HIT_K, PRECISION_K);
		System.out.println("═".repeat(72));
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
	@DisplayName("전체 정확도 비교 (Hit@3 / MRR / Precision@5)")
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

	// ══════════════════════════════════════════════════════════════════════════
	// 지표 계산
	// ══════════════════════════════════════════════════════════════════════════

	private ComparisonResult runSingleComparison(TestCase tc) {

		// ── MySQL ──
		long t0 = System.nanoTime();
		List<Post> mysqlResults = postRepository.searchPostsByCondition(
			null, null, null, tc.query());
		long mysqlMs = (System.nanoTime() - t0) / 1_000_000;

		List<Boolean> mysqlRelevance = mysqlResults.stream()
			.map(p -> isRelevant(p.getTitle(), p.getContent(), tc))
			.toList();

		// ── ES ──
		long t1 = System.nanoTime();
		List<PostDocument> esResults = postSearchService.search(
			null, null, null, tc.query());
		long esMs = (System.nanoTime() - t1) / 1_000_000;

		List<Boolean> esRelevance = esResults.stream()
			.map(d -> isRelevant(d.getTitle(), d.getContent(), tc))
			.toList();

		return new ComparisonResult(
			tc,
			hitAtK(mysqlRelevance, HIT_K),   esHitAtK(esRelevance, HIT_K, tc),
			mrr(mysqlRelevance),              mrr(esRelevance),
			precisionAtK(mysqlRelevance, PRECISION_K), precisionAtK(esRelevance, PRECISION_K),
			mysqlResults.size(), esResults.size(),
			mysqlMs, esMs
		);
	}

	/**
	 * 해당 게시글이 테스트 케이스에서 관련 문서인지 판정.
	 * - 검색 불가 케이스: 항상 false (0건이어야 정상이므로)
	 * - 일반 케이스: 제목 또는 내용에 expectedKeywords 중 하나 이상 포함
	 */
	private boolean isRelevant(String title, String content, TestCase tc) {
		if (tc.isNoResultCase()) return false;
		return tc.expectedKeywords().stream()
			.anyMatch(kw -> title.contains(kw) || content.contains(kw));
	}

	/**
	 * Hit@K: 상위 K개 결과 안에 관련 문서가 하나라도 있으면 true.
	 * 검색 불가 케이스는 결과가 0건일 때 true (특이도 기준).
	 */
	private boolean hitAtK(List<Boolean> relevance, int k) {
		return relevance.stream().limit(k).anyMatch(Boolean::booleanValue);
	}

	private boolean esHitAtK(List<Boolean> relevance, int k, TestCase tc) {
		if (tc.isNoResultCase()) return relevance.isEmpty();
		return hitAtK(relevance, k);
	}

	/**
	 * MRR: 첫 번째 관련 문서의 순위 역수.
	 * 관련 문서가 없으면 0.
	 */
	private double mrr(List<Boolean> relevance) {
		return IntStream.range(0, relevance.size())
			.filter(i -> relevance.get(i))
			.mapToDouble(i -> 1.0 / (i + 1))
			.findFirst()
			.orElse(0.0);
	}

	/**
	 * Precision@K: 상위 K개 중 관련 문서의 비율.
	 * 결과가 K개 미만이면 실제 결과 수로 나눔.
	 */
	private double precisionAtK(List<Boolean> relevance, int k) {
		if (relevance.isEmpty()) return 0.0;
		long relevant = relevance.stream().limit(k).filter(Boolean::booleanValue).count();
		int denominator = Math.min(relevance.size(), k);
		return (double) relevant / denominator;
	}

	// ══════════════════════════════════════════════════════════════════════════
	// 출력
	// ══════════════════════════════════════════════════════════════════════════

	private void printResult(ComparisonResult r) {
		String expected = r.testCase().isNoResultCase()
			? "[0건이어야 정상]"
			: r.testCase().expectedKeywords().toString();

		System.out.printf("질문: \"%s\" | 기대: %s%n", r.testCase().query(), expected);
		System.out.printf("  MySQL │ Hit@%d: %s │ MRR: %.2f │ Prec@%d: %.2f │ %2d건 │ %dms%n",
			HIT_K, r.mysqlHitAt3() ? "✅" : "❌",
			r.mysqlMrr(), PRECISION_K, r.mysqlPrecAt5(),
			r.mysqlCount(), r.mysqlMs());
		System.out.printf("  ES    │ Hit@%d: %s │ MRR: %.2f │ Prec@%d: %.2f │ %2d건 │ %dms%n%n",
			HIT_K, r.esHitAt3() ? "✅" : "❌",
			r.esMrr(), PRECISION_K, r.esPrecAt5(),
			r.esCount(), r.esMs());
	}

	private void printFinalReport() {
		System.out.println();
		System.out.println("═".repeat(72));
		System.out.println("                    정확도 비교 종합 리포트");
		System.out.println("═".repeat(72));

		// 검색 불가 케이스는 별도 집계
		List<ComparisonResult> searchable = allResults.stream()
			.filter(r -> !r.testCase().isNoResultCase()).toList();
		List<ComparisonResult> noResult = allResults.stream()
			.filter(r -> r.testCase().isNoResultCase()).toList();

		int total = searchable.size();

		long mHit = searchable.stream().filter(ComparisonResult::mysqlHitAt3).count();
		long eHit = searchable.stream().filter(ComparisonResult::esHitAt3).count();

		double mMrr = searchable.stream().mapToDouble(ComparisonResult::mysqlMrr).average().orElse(0);
		double eMrr = searchable.stream().mapToDouble(ComparisonResult::esMrr).average().orElse(0);

		double mPrec = searchable.stream().mapToDouble(ComparisonResult::mysqlPrecAt5).average().orElse(0);
		double ePrec = searchable.stream().mapToDouble(ComparisonResult::esPrecAt5).average().orElse(0);

		System.out.printf("%n  대상 질문: %d개 (검색 불가 %d개 별도)%n%n", total, noResult.size());

		System.out.printf("  %-12s │ %10s │ %10s%n", "지표", "MySQL", "ES");
		System.out.println("  " + "─".repeat(40));
		System.out.printf("  %-12s │ %8d/%d  │ %8d/%d%n",
			"Hit@" + HIT_K, mHit, total, eHit, total);
		System.out.printf("  %-12s │ %10.1f%% │ %10.1f%%%n",
			"Hit@" + HIT_K + " 정확도", pct(mHit, total), pct(eHit, total));
		System.out.printf("  %-12s │ %10.3f │ %10.3f%n", "MRR", mMrr, eMrr);
		System.out.printf("  %-12s │ %10.3f │ %10.3f%n", "Prec@" + PRECISION_K, mPrec, ePrec);

		// 카테고리별 Hit@K
		System.out.printf("%n  [ 카테고리별 Hit@%d ]%n", HIT_K);
		searchable.stream()
			.collect(Collectors.groupingBy(r -> r.testCase().category(),
				LinkedHashMap::new, Collectors.toList()))
			.forEach((cat, results) -> {
				long m = results.stream().filter(ComparisonResult::mysqlHitAt3).count();
				long e = results.stream().filter(ComparisonResult::esHitAt3).count();
				System.out.printf("  %-12s — MySQL: %d/%d | ES: %d/%d%n",
					cat, m, results.size(), e, results.size());
			});

		// ES만 성공한 케이스 (ES 우위의 핵심 근거)
		List<ComparisonResult> esOnly = searchable.stream()
			.filter(r -> r.esHitAt3() && !r.mysqlHitAt3()).toList();
		if (!esOnly.isEmpty()) {
			System.out.printf("%n  [ ES만 Hit@%d 성공 (MySQL 실패): %d개 — ES 우위 케이스 ]%n",
				HIT_K, esOnly.size());
			esOnly.forEach(r -> System.out.printf(
				"  • \"%s\" — ES %d건(MRR %.2f) vs MySQL %d건(MRR %.2f)%n",
				r.testCase().query(), r.esCount(), r.esMrr(), r.mysqlCount(), r.mysqlMrr()));
		}

		// 검색 불가 특이도
		long mSpec = noResult.stream().filter(r -> r.mysqlCount() == 0).count();
		long eSpec = noResult.stream().filter(r -> r.esCount() == 0).count();
		System.out.printf("%n  [ 검색 불가 특이도 (0건 처리) ]%n");
		System.out.printf("  MySQL: %d/%d | ES: %d/%d%n",
			mSpec, noResult.size(), eSpec, noResult.size());

		System.out.println("═".repeat(72));
	}

	private double pct(long hit, int total) {
		return total > 0 ? (double) hit / total * 100 : 0;
	}
}