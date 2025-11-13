package com.fromm.leafmap.domain.member.service;

import com.fromm.leafmap.domain.member.dto.MemberRequestDTO;
import com.fromm.leafmap.domain.member.entity.Member;

public interface MemberService {
    public Member signup(MemberRequestDTO.MemberSignupDTO memberSignupDto);
}
