package com.fromm.leafmap.domain.post.service;

import com.fromm.leafmap.domain.comment.dto.CommentResponseDTO;
import com.fromm.leafmap.domain.major.dto.MajorResponseDTO;
import com.fromm.leafmap.domain.member.dto.MemberResponseDTO;
import com.fromm.leafmap.domain.member.entity.Member;
import com.fromm.leafmap.domain.post.dto.PostRequestDTO;
import com.fromm.leafmap.domain.post.dto.PostResponseDTO;
import com.fromm.leafmap.domain.post.entity.BoardType;
import com.fromm.leafmap.domain.post.entity.Post;
import com.fromm.leafmap.domain.post.repository.PostLikeRepository;
import com.fromm.leafmap.domain.post.repository.PostRepository;
import com.fromm.leafmap.global.apiPayload.code.status.ErrorStatus;
import com.fromm.leafmap.global.apiPayload.exception.handler.ErrorHandler;
import com.fromm.leafmap.global.s3.S3Uploader;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
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
                .postId(post.getId())
                .build();
    }

    @Override
    @Transactional
    public PostResponseDTO.PostListResultDTO getPostList(BoardType boardType, Member member, Long cursor, int limit) {

        if (cursor == null || cursor == 0) {
            cursor = Long.MAX_VALUE; // 첫 페이지 처리
        }

        List<Post> posts = postRepository.findPostList(boardType, cursor, PageRequest.of(0, limit));

        List<PostResponseDTO.PostPreviewDTO> previews = posts.stream()
                .map(post -> {
                    PostResponseDTO.PostPreviewDTO.PostPreviewDTOBuilder builder = PostResponseDTO.PostPreviewDTO.builder()
                            .postId(post.getId())
                            .title(post.getTitle())
                            .contentPreview(extractFirstLine(post.getContent()));

                    // MAJOR_TIPS 게시판인 경우 Major 정보 포함
                    if (boardType == BoardType.MAJOR_TIPS && post.getMajor() != null) {
                        builder.majorId(post.getMajor().getId())
                               .majorName(post.getMajor().getName());
                    }

                    return builder.build();
                })
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

    @Override
    @Transactional
    public PostResponseDTO.PostDetailResultDTO getPostDetail(BoardType boardType, Long postId, Member member) {
        Post post = postRepository.findByIdAndBoardType(postId, boardType)
                .orElseThrow(() -> new ErrorHandler(ErrorStatus.POST_NOT_FOUND));

        // 게시글 작성자 여부
        boolean isPostWriter = member != null && post.getMember() != null
                && member.getId().equals(post.getMember().getId());

        // 게시글 좋아요 여부
        boolean isLiked = member != null
                && postLikeRepository.existsByPostIdAndMemberId(postId, member.getId());

        // CommentDTO
        List<CommentResponseDTO.CommentDTO> commentDTOs = post.getComments().stream()
                .map(c -> {
                    boolean isCommentWriter = member != null && c.getMember() != null
                            && member.getId().equals(c.getMember().getId());

                    return CommentResponseDTO.CommentDTO.builder()
                            .commentId(c.getId())
                            .parentId(c.getParent() != null ? c.getParent().getId() : null)
                            .content(c.getContent())
                            .nickname(c.getMember().getNickname())
                            .isWriter(isCommentWriter)
                            .createdAt(c.getCreatedAt())
                            .build();
                })
                .toList();

        // MemberDTO
        MemberResponseDTO.MemberDTO memberDTO = null;
        if (post.getMember() != null) {
            memberDTO = MemberResponseDTO.MemberDTO.builder()
                    .id(post.getMember().getId())
                    .nickname(post.getMember().getNickname())
                    .build();
        }

        // MajorDTO (MAJOR_TIPS 게시판)
        MajorResponseDTO.MajorDTO majorDTO = null;
        if (boardType == BoardType.MAJOR_TIPS && post.getMajor() != null) {
            majorDTO = MajorResponseDTO.MajorDTO.builder()
                    .id(post.getMajor().getId())
                    .name(post.getMajor().getName())
                    .description(post.getMajor().getDescription())
                    .keywords(post.getMajor().getKeywords())
                    .career(post.getMajor().getCareer())
                    .curriculumUrl(post.getMajor().getCurriculumUrl())
                    .build();
        }

        return PostResponseDTO.PostDetailResultDTO.builder()
                .postId(post.getId())
                .boardType(post.getBoardType())
                .title(post.getTitle())
                .content(post.getContent())
                .address(post.getAddress())
                .imageUrl(post.getImageUrl())
                .isPublic(post.getIsPublic())
                .likeCount(post.getLikeCount())
                .badge(post.getBadge())
                .createdAt(post.getCreatedAt())
                .isWriter(isPostWriter)
                .isLiked(isLiked)
                .member(memberDTO)
                .major(majorDTO)
                .comments(commentDTOs)
                .build();
    }
}
