package com.shh.shhbook.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LikeResponse {
    private boolean liked;
    private int likeCount;

    public LikeResponse(boolean liked, int likeCount) {
        this.liked = liked;
        this.likeCount = likeCount;
    }
}
