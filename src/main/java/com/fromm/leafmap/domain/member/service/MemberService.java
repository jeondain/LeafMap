package com.fromm.leafmap.domain.member.service;

import com.fromm.leafmap.domain.member.dto.MemberRequestDTO;
import com.fromm.leafmap.domain.member.dto.MemberResponseDTO;
import com.fromm.leafmap.domain.member.entity.Member;

public interface MemberService {

    public Member signup(MemberRequestDTO.MemberSignupDTO memberSignupDto);

    public MemberResponseDTO.MemberLoginResultDTO login(MemberRequestDTO.MemberLoginDTO memberLoginDto);

    public MemberResponseDTO.GetMemberInfoResultDTO getMemberInfo(Member member);
}
