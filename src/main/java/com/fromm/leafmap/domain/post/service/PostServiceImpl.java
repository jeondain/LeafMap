package com.fromm.leafmap.domain.post.service;

import com.fromm.leafmap.domain.member.entity.Member;
import com.fromm.leafmap.domain.post.dto.PostRequestDTO;
import com.fromm.leafmap.domain.post.dto.PostResponseDTO;
import com.fromm.leafmap.domain.post.entity.BoardType;
import com.fromm.leafmap.domain.post.entity.Post;
import com.fromm.leafmap.domain.post.repository.PostRepository;
import com.fromm.leafmap.global.s3.S3Uploader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;
    private final S3Uploader s3Uploader;

    @Override
    @Transactional
    public PostResponseDTO.AddPostResultDTO addPost(BoardType boardType, PostRequestDTO.AddPostRequestDTO request, MultipartFile image, Member member) {

        String imageUrl = null;
        if (image != null && !image.isEmpty()) {
            imageUrl = s3Uploader.upload(image, "post-images");
        }

        Post post = Post.builder()
                .boardType(boardType)
                .title(request.getTitle())
                .content(request.getContent())
                .address(request.getAddress())
                .imageUrl(imageUrl)
                .isPublic(false)
                .member(member)
                .likeCount(0)
                .badge(false)
                .build();

        postRepository.save(post);
        return PostResponseDTO.AddPostResultDTO.builder()
                .id(post.getId())
                .build();
    }
}
