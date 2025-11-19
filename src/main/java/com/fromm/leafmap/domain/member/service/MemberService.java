package com.fromm.leafmap.domain.member.service;

import com.fromm.leafmap.domain.member.dto.MemberRequestDTO;
import com.fromm.leafmap.domain.member.dto.MemberResponseDTO;
import com.fromm.leafmap.domain.member.entity.Member;
import com.fromm.leafmap.domain.post.dto.PostResponseDTO;

public interface MemberService {

    Member signup(MemberRequestDTO.MemberSignupDTO memberSignupDto);

    MemberResponseDTO.MemberLoginResultDTO login(MemberRequestDTO.MemberLoginDTO memberLoginDto);

    MemberResponseDTO.GetMemberInfoResultDTO getMemberInfo(Member member);

    MemberResponseDTO.GetMemberInfoResultDTO updateMemberInfo(MemberRequestDTO.UpdateMemberInfoDTO request, Member member);

    PostResponseDTO.PostListResultDTO getMemberPosts(Member member, Long cursor, int limit);

    PostResponseDTO.PostListResultDTO getPostsLikedByMember(Member member, Long cursor, int limit);
}
