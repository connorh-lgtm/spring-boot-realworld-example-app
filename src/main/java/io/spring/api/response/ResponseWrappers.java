package io.spring.api.response;

import io.spring.application.data.ArticleData;
import io.spring.application.data.CommentData;
import io.spring.application.data.ProfileData;
import io.spring.application.data.UserWithToken;
import java.util.List;

public final class ResponseWrappers {
  private ResponseWrappers() {}

  public record ArticleResponse(ArticleData article) {}

  public record CommentResponse(CommentData comment) {}

  public record CommentsResponse(List<CommentData> comments) {}

  public record ProfileResponse(ProfileData profile) {}

  public record UserResponse(UserWithToken user) {}

  public record TagsResponse(List<String> tags) {}

  public record ErrorMessageResponse(String message) {}
}
