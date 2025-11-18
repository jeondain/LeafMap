package com.fromm.leafmap.domain.post.service;

import com.fromm.leafmap.domain.member.entity.Member;
import com.fromm.leafmap.domain.post.dto.PostLikeResponseDTO;
import com.fromm.leafmap.domain.post.entity.BoardType;

public interface PostLikeService {
    public PostLikeResponseDTO.PostLikeResultDTO toggleLike(BoardType boardType, Long postId, Member member);
}
