package cm.aptoide.pt.v8engine.repository.request;

import cm.aptoide.accountmanager.AptoideAccountManager;
import cm.aptoide.pt.dataprovider.ws.v7.BodyInterceptor;
import cm.aptoide.pt.dataprovider.ws.v7.ListAppsRequest;
import cm.aptoide.pt.dataprovider.ws.v7.ListFullReviewsRequest;
import cm.aptoide.pt.dataprovider.ws.v7.store.GetStoreRequest;
import cm.aptoide.pt.dataprovider.ws.v7.store.GetStoreWidgetsRequest;
import cm.aptoide.pt.dataprovider.ws.v7.store.GetUserRequest;
import cm.aptoide.pt.dataprovider.ws.v7.store.ListStoresRequest;
import cm.aptoide.pt.interfaces.AptoideClientUUID;
import cm.aptoide.pt.v8engine.interfaces.StoreCredentialsProvider;

/**
 * Created by neuro on 26-12-2016.
 */

public class RequestFactory {

  private final ListStoresRequestFactory listStoresRequestFactory;
  private final ListAppsRequestFactory listAppsRequestFactory;
  private final ListFullReviewsRequestFactory listFullReviewsRequestFactory;
  private final GetStoreRequestFactory getStoreRequestFactory;
  private final GetStoreWidgetsRequestFactory getStoreWidgetsRequestFactory;
  private final StoreCredentialsProvider storeCredentialsProvider;
  private final GetUserRequestFactory getUserRequestFactory;

  public RequestFactory(AptoideClientUUID aptoideClientUUID, AptoideAccountManager accountManager,
      StoreCredentialsProvider storeCredentialsProvider, BodyInterceptor bodyInterceptor) {
    this.storeCredentialsProvider = storeCredentialsProvider;
    listStoresRequestFactory =
        new ListStoresRequestFactory(aptoideClientUUID, accountManager, bodyInterceptor);
    listAppsRequestFactory =
        new ListAppsRequestFactory(bodyInterceptor, storeCredentialsProvider);
    listFullReviewsRequestFactory =
        new ListFullReviewsRequestFactory(aptoideClientUUID, accountManager, bodyInterceptor);
    getStoreRequestFactory =
        new GetStoreRequestFactory(accountManager, storeCredentialsProvider, bodyInterceptor);
    getStoreWidgetsRequestFactory =
        new GetStoreWidgetsRequestFactory(accountManager, storeCredentialsProvider, bodyInterceptor);
    getUserRequestFactory = new GetUserRequestFactory(bodyInterceptor);
  }

  public ListStoresRequest newListStoresRequest(int offset, int limit) {
    return this.listStoresRequestFactory.newListStoresRequest(offset, limit);
  }

  public ListStoresRequest newListStoresRequest(String url) {
    return this.listStoresRequestFactory.newListStoresRequest(url);
  }

  public ListAppsRequest newListAppsRequest(String url) {
    return this.listAppsRequestFactory.newListAppsRequest(url);
  }

  public ListFullReviewsRequest newListFullReviews(String url, boolean refresh) {
    return this.listFullReviewsRequestFactory.newListFullReviews(url, refresh,
        storeCredentialsProvider.fromUrl(url));
  }

  public GetStoreRequest newStore(String url) {
    return this.getStoreRequestFactory.newStore(url);
  }

  public GetStoreWidgetsRequest newStoreWidgets(String url) {
    return this.getStoreWidgetsRequestFactory.newStoreWidgets(url);
  }

  public GetUserRequest newGetUser(String url) {
    return this.getUserRequestFactory.newGetUser(url);
  }
}
