package com.fromm.leafmap.domain.post.service;

import com.fromm.leafmap.domain.member.entity.Member;
import com.fromm.leafmap.domain.post.dto.PostRequestDTO;
import com.fromm.leafmap.domain.post.dto.PostResponseDTO;
import com.fromm.leafmap.domain.post.entity.BoardType;
import org.springframework.web.multipart.MultipartFile;

public interface PostService {
    PostResponseDTO.AddPostResultDTO addPost(BoardType boardType, PostRequestDTO.AddPostRequestDTO request, MultipartFile image, Member member);
}
