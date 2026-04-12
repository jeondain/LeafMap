package com.fromm.leafmap.domain.post.event;

import com.fromm.leafmap.domain.post.entity.Post;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PostUpdatedEvent {
    private final Post post;
}
