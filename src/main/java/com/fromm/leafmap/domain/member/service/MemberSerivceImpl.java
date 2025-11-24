package com.fromm.leafmap.domain.member.service;

import com.fromm.leafmap.domain.major.entity.Major;
import com.fromm.leafmap.domain.major.repository.MajorRepository;
import com.fromm.leafmap.domain.member.dto.MemberRequestDTO;
import com.fromm.leafmap.domain.member.dto.MemberResponseDTO;
import com.fromm.leafmap.domain.member.entity.Member;
import com.fromm.leafmap.domain.member.entity.Role;
import com.fromm.leafmap.domain.member.repository.MemberRepository;
import com.fromm.leafmap.domain.post.dto.PostResponseDTO;
import com.fromm.leafmap.domain.post.entity.Post;
import com.fromm.leafmap.domain.post.repository.PostRepository;
import com.fromm.leafmap.global.apiPayload.code.status.ErrorStatus;
import com.fromm.leafmap.global.apiPayload.exception.handler.ErrorHandler;
import com.fromm.leafmap.global.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MemberSerivceImpl implements MemberService {

    private final MemberRepository memberRepository;
    private final MajorRepository majorRepository;
    private final PostRepository postRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Override
    @Transactional
    public Member signup(MemberRequestDTO.MemberSignupDTO memberSignupDto) {
        Optional<Member> optionalMember = memberRepository.findByLoginId(memberSignupDto.getLoginId());

        if (optionalMember.isPresent()) {
           throw new ErrorHandler(ErrorStatus.MEMBER_ALREADY_EXISTS);
        }

        String encodedPassword = passwordEncoder.encode(memberSignupDto.getPassword());

        Major major = majorRepository.findByName(memberSignupDto.getMajor())
                .orElseThrow(() -> new ErrorHandler(ErrorStatus.MAJOR_NOT_FOUND));

        Major desiredMajor = majorRepository.findByName(memberSignupDto.getDesiredMajor())
                .orElseThrow(() -> new ErrorHandler(ErrorStatus.MAJOR_NOT_FOUND));

        Member member = Member.builder()
                .loginId(memberSignupDto.getLoginId())
                .password(encodedPassword)
                .nickname(memberSignupDto.getNickname())
                .studentId(memberSignupDto.getStudentId())
                .major(major)
                .desiredMajor(desiredMajor)
                .role(Role.USER)
                .build();

        return memberRepository.save(member);
    }

    @Override
    @Transactional
    public MemberResponseDTO.MemberLoginResultDTO login(MemberRequestDTO.MemberLoginDTO memberLoginDto) {
        Member member = memberRepository.findByLoginId(memberLoginDto.getLoginId())
                .orElseThrow(() -> new ErrorHandler(ErrorStatus.MEMBER_NOT_FOUND));

        if (!passwordEncoder.matches(memberLoginDto.getPassword(), member.getPassword())) {
            throw new ErrorHandler(ErrorStatus.INVALID_PASSWORD);
        }

        String accessToken = jwtService.createAccessToken(member.getLoginId());

        return MemberResponseDTO.MemberLoginResultDTO.builder()
                .memberId(member.getId())
                .accessToken(accessToken)
                .build();
    }

    @Override
    @Transactional
    public MemberResponseDTO.GetMemberInfoResultDTO getMemberInfo(Member member) {
        return MemberResponseDTO.GetMemberInfoResultDTO.builder()
                .memberId(member.getId())
                .nickname(member.getNickname())
                .major(member.getMajor() != null ? member.getMajor().getName() : null)
                .desiredMajor(member.getDesiredMajor() != null ? member.getDesiredMajor().getName() : null)
                .build();
    }

    @Override
    @Transactional
    public MemberResponseDTO.GetMemberInfoResultDTO updateMemberInfo( MemberRequestDTO.UpdateMemberInfoDTO request, Member member) {
        // 닉네임 업데이트
        if (request.getNickname() != null) {
            member.setNickname(request.getNickname());
        }

        // 전공 업데이트
        if (request.getMajor() != null) {
            Major major = majorRepository.findByName(request.getMajor())
                    .orElseThrow(() -> new ErrorHandler(ErrorStatus.MAJOR_NOT_FOUND));
            member.setMajor(major);
        }

        // 희망 전공 업데이트
        if (request.getDesiredMajor() != null) {
            Major desiredMajor = majorRepository.findByName(request.getDesiredMajor())
                    .orElseThrow(() -> new ErrorHandler(ErrorStatus.MAJOR_NOT_FOUND));
            member.setDesiredMajor(desiredMajor);
        }

        memberRepository.save(member);
        return MemberResponseDTO.GetMemberInfoResultDTO.builder()
                .memberId(member.getId())
                .nickname(member.getNickname())
                .major(member.getMajor() != null ? member.getMajor().getName() : null)
                .desiredMajor(member.getDesiredMajor() != null ? member.getDesiredMajor().getName() : null)
                .build();
    }

    @Override
    @Transactional
    public PostResponseDTO.PostListResultDTO getMemberPosts(Member member, Long cursor, int limit) {

        if (cursor == null || cursor == 0) {
            cursor = Long.MAX_VALUE; // 첫 페이지 처리
        }

        List<Post> posts = postRepository.findMyPostList(member.getId(), cursor, PageRequest.of(0, limit));

        return buildPostListResultDTO(posts, limit);
    }

    @Override
    @Transactional
    public PostResponseDTO.PostListResultDTO getPostsLikedByMember(Member member, Long cursor, int limit) {

        if (cursor == null || cursor == 0) {
            cursor = Long.MAX_VALUE; // 첫 페이지 처리
        }

        List<Post> posts = postRepository.findPostsLikedByMember(member.getId(), cursor, PageRequest.of(0, limit));

        return buildPostListResultDTO(posts, limit);
    }

    @Override
    @Transactional
    public PostResponseDTO.PostListResultDTO getPostsCommentedByMember(Member member, Long cursor, int limit) {

        if (cursor == null || cursor == 0) {
            cursor = Long.MAX_VALUE; // 첫 페이지 처리
        }

        List<Post> posts = postRepository.findPostsCommentedByMember(member.getId(), cursor, PageRequest.of(0, limit));

        return buildPostListResultDTO(posts, limit);
    }

    private PostResponseDTO.PostListResultDTO buildPostListResultDTO(List<Post> posts, int limit) {

        List<PostResponseDTO.PostPreviewDTO> previews = posts.stream()
                .map(post -> PostResponseDTO.PostPreviewDTO.builder()
                        .postId(post.getId())
                        .title(post.getTitle())
                        .contentPreview(extractFirstLine(post.getContent()))
                        .badge(post.getBadge())
                        .isPublic(post.getIsPublic())
                        .boardType(post.getBoardType())
                        .build())
                .toList();

        Long nextCursor = posts.isEmpty() ? null : posts.get(posts.size() - 1).getId();
        boolean hasNext = posts.size() == limit;

        return PostResponseDTO.PostListResultDTO.builder()
                .posts(previews)
                .nextCursor(nextCursor)
                .hasNext(hasNext)
                .build();
    }

    private String extractFirstLine(String content) {
        if (content == null || content.isBlank()) return "";
        return content.split("\n")[0];
    }
}
