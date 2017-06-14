/*
 * Copyright (c) 2016.
 * Modified on 04/08/2016.
 */

package cm.aptoide.pt.dataprovider.ws.v7;

import cm.aptoide.pt.dataprovider.interfaces.TokenInvalidator;
import cm.aptoide.pt.dataprovider.ws.BodyInterceptor;
import cm.aptoide.pt.model.v7.GetApp;
import cm.aptoide.pt.preferences.managed.ManagerPreferences;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import okhttp3.OkHttpClient;
import retrofit2.Converter;
import rx.Observable;

/**
 * Created by neuro on 22-04-2016.
 */
@EqualsAndHashCode(callSuper = true) public class GetAppRequest
    extends V7<GetApp, GetAppRequest.Body> {

  private GetAppRequest(String baseHost, Body body, BodyInterceptor<BaseBody> bodyInterceptor,
      OkHttpClient httpClient, Converter.Factory converterFactory,
      TokenInvalidator tokenInvalidator) {
    super(body, baseHost, httpClient, converterFactory, bodyInterceptor, tokenInvalidator);
  }

  public static GetAppRequest of(String packageName, String storeName,
      BodyInterceptor<BaseBody> bodyInterceptor, OkHttpClient httpClient,
      Converter.Factory converterFactory, TokenInvalidator tokenInvalidator) {

    boolean forceServerRefresh = ManagerPreferences.getAndResetForceServerRefresh();

    return new GetAppRequest(BASE_HOST, new Body(packageName, storeName, forceServerRefresh),
        bodyInterceptor, httpClient, converterFactory, tokenInvalidator);
  }

  public static GetAppRequest of(String packageName, BodyInterceptor<BaseBody> bodyInterceptor,
      long appId, OkHttpClient httpClient, Converter.Factory converterFactory,
      TokenInvalidator tokenInvalidator) {
    boolean forceServerRefresh = ManagerPreferences.getAndResetForceServerRefresh();

    return new GetAppRequest(BASE_HOST, new Body(appId, forceServerRefresh, packageName),
        bodyInterceptor, httpClient, converterFactory, tokenInvalidator);
  }

  public static GetAppRequest ofMd5(String md5, BodyInterceptor<BaseBody> bodyInterceptor,
      OkHttpClient httpClient, Converter.Factory converterFactory,
      TokenInvalidator tokenInvalidator) {
    boolean forceServerRefresh = ManagerPreferences.getAndResetForceServerRefresh();

    return new GetAppRequest(BASE_HOST, new Body(forceServerRefresh, md5), bodyInterceptor,
        httpClient, converterFactory, tokenInvalidator);
  }

  public static GetAppRequest ofUname(String uname, BodyInterceptor<BaseBody> bodyInterceptor,
      OkHttpClient httpClient, Converter.Factory converterFactory,
      TokenInvalidator tokenInvalidator) {

    return new GetAppRequest(BASE_HOST, new Body(uname), bodyInterceptor, httpClient,
        converterFactory, tokenInvalidator);
  }

  public static GetAppRequest of(long appId, String storeName,
      BaseRequestWithStore.StoreCredentials storeCredentials, String packageName,
      BodyInterceptor<BaseBody> bodyInterceptor, OkHttpClient httpClient,
      Converter.Factory converterFactory, TokenInvalidator tokenInvalidator) {

    boolean forceServerRefresh = ManagerPreferences.getAndResetForceServerRefresh();

    Body body = new Body(appId, storeName, forceServerRefresh, packageName);
    body.setStoreUser(storeCredentials.getUsername());
    body.setStorePassSha1(storeCredentials.getPasswordSha1());

    return new GetAppRequest(BASE_HOST, body, bodyInterceptor, httpClient, converterFactory,
        tokenInvalidator);
  }

  @Override
  protected Observable<GetApp> loadDataFromNetwork(Interfaces interfaces, boolean bypassCache) {
    return interfaces.getApp(body, bypassCache);
  }

  @EqualsAndHashCode(callSuper = true) public static class Body extends BaseBodyWithApp {

    @Getter private Long appId;
    @Getter private String packageName;
    @Getter private boolean refresh;
    @Setter @JsonProperty("package_uname") private String uname;
    @Getter @JsonProperty("apk_md5sum") private String md5;
    @Getter @JsonProperty("store_name") private String storeName;
    @Getter private Node nodes;

    public Body(Long appId, Boolean refresh, String packageName) {
      this.appId = appId;
      this.refresh = refresh;
      nodes = new Node(appId, packageName);
    }

    public Body(Long appId, String storeName, Boolean refresh, String packageName) {
      this.appId = appId;
      this.refresh = refresh;
      this.storeName = storeName;
      nodes = new Node(appId, packageName);
    }

    public Body(String packageName, String storeName, boolean refresh) {
      this.packageName = packageName;
      this.refresh = refresh;
      this.storeName = storeName;
    }

    public Body(String packageName, Boolean refresh) {
      this.packageName = packageName;
      this.refresh = refresh;
    }

    public Body(Boolean refresh, String md5) {
      this.md5 = md5;
      this.refresh = refresh;
    }

    public Body(String uname) {
      this.uname = uname;
    }

    public Body(long appId) {
      // TODO: 27/12/2016 analara
      this.appId = appId;
    }

    @Data private static class Node {

      private Meta meta;
      private Versions versions;

      public Node(long appId, String packageName) {
        this.meta = new Meta().setAppId(appId);
        this.versions = new Versions().setPackageName(packageName);
      }

      @Data @Accessors(chain = true) private static class Meta {
        private long appId;
      }

      @Data @Accessors(chain = true) private static class Versions {
        private String packageName;
      }
    }
  }
}
