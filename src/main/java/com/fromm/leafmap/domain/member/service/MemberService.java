package com.fromm.leafmap.domain.member.service;

import com.fromm.leafmap.domain.member.dto.MemberRequestDTO;
import com.fromm.leafmap.domain.member.dto.MemberResponseDTO;
import com.fromm.leafmap.domain.member.entity.Member;

public interface MemberService {

    Member signup(MemberRequestDTO.MemberSignupDTO memberSignupDto);

    MemberResponseDTO.MemberLoginResultDTO login(MemberRequestDTO.MemberLoginDTO memberLoginDto);

    MemberResponseDTO.GetMemberInfoResultDTO getMemberInfo(Member member);

    MemberResponseDTO.GetMemberInfoResultDTO updateMemberInfo(MemberRequestDTO.UpdateMemberInfoDTO request, Member member);
}
