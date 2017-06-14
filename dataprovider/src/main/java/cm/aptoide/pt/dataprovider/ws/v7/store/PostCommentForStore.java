package cm.aptoide.pt.dataprovider.ws.v7.store;

import cm.aptoide.pt.dataprovider.BuildConfig;
import cm.aptoide.pt.dataprovider.interfaces.TokenInvalidator;
import cm.aptoide.pt.dataprovider.ws.BodyInterceptor;
import cm.aptoide.pt.dataprovider.ws.v7.BaseBody;
import cm.aptoide.pt.dataprovider.ws.v7.V7;
import cm.aptoide.pt.model.v7.SetComment;
import cm.aptoide.pt.preferences.toolbox.ToolboxManager;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import okhttp3.OkHttpClient;
import retrofit2.Converter;
import rx.Observable;

public class PostCommentForStore extends V7<SetComment, PostCommentForStore.Body> {

  private static final String BASE_HOST = (ToolboxManager.isToolboxEnableHttpScheme() ? "http"
      : BuildConfig.APTOIDE_WEB_SERVICES_SCHEME)
      + "://"
      + BuildConfig.APTOIDE_WEB_SERVICES_WRITE_V7_HOST
      + "/api/7/";

  private PostCommentForStore(Body body, BodyInterceptor<BaseBody> bodyInterceptor,
      OkHttpClient httpClient, Converter.Factory converterFactory,
      TokenInvalidator tokenInvalidator) {
    super(body, BASE_HOST, httpClient, converterFactory, bodyInterceptor, tokenInvalidator);
  }

  public static PostCommentForStore of(long storeId, String text,
      BodyInterceptor<BaseBody> bodyInterceptor, OkHttpClient httpClient,
      Converter.Factory converterFactory, TokenInvalidator tokenInvalidator) {
    Body body = new Body(storeId, text);
    return new PostCommentForStore(body, bodyInterceptor, httpClient, converterFactory,
        tokenInvalidator);
  }

  public static PostCommentForStore of(long storeId, long previousCommentId, String text,
      BodyInterceptor<BaseBody> bodyInterceptor, OkHttpClient httpClient,
      Converter.Factory converterFactory, TokenInvalidator tokenInvalidator) {
    Body body = new Body(storeId, text, previousCommentId);
    return new PostCommentForStore(body, bodyInterceptor, httpClient, converterFactory,
        tokenInvalidator);
  }

  @Override
  protected Observable<SetComment> loadDataFromNetwork(Interfaces interfaces, boolean bypassCache) {
    return interfaces.postStoreComment(body, true);
  }

  @Data @Accessors(chain = false) @EqualsAndHashCode(callSuper = true) public static class Body
      extends BaseBody {

    private long storeId;
    @JsonProperty("comment_id") private Long previousCommentId;
    private String body;
    //private String commentType = CommentType.STORE.name();

    public Body(long storeId, String text, long previousCommentId) {
      this(storeId, text);
      this.previousCommentId = previousCommentId;
    }

    public Body(long storeId, String text) {
      this.storeId = storeId;
      this.body = text;
    }
  }
}
