package cm.aptoide.pt.search.view;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import cm.aptoide.aptoideviews.filters.Filter;
import cm.aptoide.pt.bottomNavigation.BottomNavigationItem;
import cm.aptoide.pt.bottomNavigation.BottomNavigationMapper;
import cm.aptoide.pt.crashreports.CrashReport;
import cm.aptoide.pt.home.AptoideBottomNavigator;
import cm.aptoide.pt.logger.Logger;
import cm.aptoide.pt.presenter.Presenter;
import cm.aptoide.pt.presenter.View;
import cm.aptoide.pt.search.SearchManager;
import cm.aptoide.pt.search.SearchNavigator;
import cm.aptoide.pt.search.SearchResultDiffModel;
import cm.aptoide.pt.search.analytics.SearchAnalytics;
import cm.aptoide.pt.search.analytics.SearchSource;
import cm.aptoide.pt.search.model.SearchAppResultWrapper;
import cm.aptoide.pt.search.model.SearchQueryModel;
import cm.aptoide.pt.search.model.SearchResult;
import cm.aptoide.pt.search.model.SearchResultCount;
import cm.aptoide.pt.search.model.SearchResultError;
import cm.aptoide.pt.search.model.Source;
import cm.aptoide.pt.search.suggestions.SearchQueryEvent;
import cm.aptoide.pt.search.suggestions.SearchSuggestionManager;
import cm.aptoide.pt.search.suggestions.TrendingManager;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import rx.Completable;
import rx.Observable;
import rx.Scheduler;
import rx.Single;
import rx.exceptions.OnErrorNotImplementedException;
import rx.schedulers.Schedulers;

@SuppressWarnings({ "WeakerAccess", "Convert2MethodRef" }) public class SearchResultPresenter
    implements Presenter {
  private static final String TAG = SearchResultPresenter.class.getName();
  private final SearchResultView view;
  private final SearchAnalytics analytics;
  private final SearchNavigator navigator;
  private final CrashReport crashReport;
  private final Scheduler viewScheduler;
  private final SearchManager searchManager;
  private final TrendingManager trendingManager;
  private final SearchSuggestionManager suggestionManager;
  private final AptoideBottomNavigator bottomNavigator;
  private final BottomNavigationMapper bottomNavigationMapper;
  private final Scheduler ioScheduler;

  public SearchResultPresenter(SearchResultView view, SearchAnalytics analytics,
      SearchNavigator navigator, CrashReport crashReport, Scheduler viewScheduler,
      SearchManager searchManager, TrendingManager trendingManager,
      SearchSuggestionManager suggestionManager, AptoideBottomNavigator bottomNavigator,
      BottomNavigationMapper bottomNavigationMapper, Scheduler ioScheduler) {
    this.view = view;
    this.analytics = analytics;
    this.navigator = navigator;
    this.crashReport = crashReport;
    this.viewScheduler = viewScheduler;
    this.searchManager = searchManager;
    this.trendingManager = trendingManager;
    this.suggestionManager = suggestionManager;
    this.bottomNavigator = bottomNavigator;
    this.bottomNavigationMapper = bottomNavigationMapper;
    this.ioScheduler = ioScheduler;
  }

  @Override public void present() {
    getTrendingOnStart();
    handleToolbarClick();
    handleSearchMenuItemClick();
    focusInSearchBar();
    handleSuggestionClicked();
    stopLoadingMoreOnDestroy();
    handleFragmentRestorationVisibility();
    doFirstSearch();
    firstAdsDataLoad();
    handleClickToOpenAppViewFromItem();
    handleClickToOpenAppViewFromAdd();
    handleSearchListReachedBottom();
    handleQueryTextSubmitted();
    handleQueryTextChanged();
    handleQueryTextCleaned();
    handleClickOnBottomNavWithResults();
    handleClickOnBottomNavWithoutResults();
    handleErrorRetryClick();
    handleFiltersClick();
    listenToSearchQueries();

    handleClickOnAdultContentSwitch();
    handleAdultContentDialogPositiveClick();
    handleAdultContentDialogNegativeClick();
    handleAdultContentDialogWithPinPositiveClick();
    redoSearchAfterAdultContentSwitch();
    updateAdultContentSwitchOnNoResults();
  }

  private void handleFiltersClick() {
    view.getLifecycleEvent()
        .filter(lifecycleEvent -> lifecycleEvent.equals(View.LifecycleEvent.CREATE))
        .flatMap(__ -> view.filtersChangeEvents()
            .map(___ -> view.getViewModel())
            .doOnNext(___ -> view.showResultsLoading())
            .flatMap(viewModel -> loadData(viewModel.getSearchQueryModel()
                    .getFinalQuery(), viewModel.getStoreName(), viewModel.getFilters(),
                false).toObservable()
                .observeOn(viewScheduler)
                .doOnNext(searchResult -> {
                  if (searchResult.hasError()) {
                    if (searchResult.getError() == SearchResultError.NO_NETWORK) {
                      view.showNoNetworkView();
                    } else {
                      view.showGenericErrorView();
                    }
                  } else {
                    if (getResultsCount(searchResult) <= 0) {
                      view.showNoResultsView();
                      analytics.searchNoResults(viewModel.getSearchQueryModel());
                    } else {
                      view.showResultsView();
                    }
                  }
                }))
            .retry())
        .doOnNext(filters -> view.getViewModel())
        .compose(view.bindUntilEvent(View.LifecycleEvent.DESTROY))
        .subscribe(__ -> {
        }, crashReport::log);
  }

  private void handleErrorRetryClick() {
    view.getLifecycleEvent()
        .filter(lifecycleEvent -> lifecycleEvent.equals(View.LifecycleEvent.CREATE))
        .flatMap(viewCreated -> view.retryClicked())
        .observeOn(viewScheduler)
        .map(__ -> view.getViewModel())
        .flatMap(model -> search(model))
        .compose(view.bindUntilEvent(View.LifecycleEvent.DESTROY))
        .subscribe(__ -> {
        }, crashReport::log);
  }

  private Completable loadBannerAd() {
    return searchManager.shouldLoadBannerAd()
        .observeOn(viewScheduler)
        .doOnSuccess(shouldLoad -> {
          if (shouldLoad) view.showBannerAd();
        })
        .toCompletable();
  }

  @VisibleForTesting public void handleFragmentRestorationVisibility() {
    view.getLifecycleEvent()
        .filter(event -> event.equals(View.LifecycleEvent.CREATE))
        .flatMap(__ -> view.searchSetup())
        .filter(__ -> !view.shouldFocusInSearchBar() && view.shouldShowSuggestions())
        .doOnNext(__ -> view.setVisibilityOnRestore())
        .compose(view.bindUntilEvent(View.LifecycleEvent.DESTROY))
        .subscribe(__ -> {
        }, e -> crashReport.log(e));
  }

  @VisibleForTesting public void getTrendingOnStart() {
    view.getLifecycleEvent()
        .filter(event -> event.equals(View.LifecycleEvent.CREATE))
        .flatMap(__ -> view.searchSetup())
        .flatMapSingle(__ -> trendingManager.getTrendingListSuggestions()
            .observeOn(viewScheduler)
            .doOnSuccess(trending -> view.setTrendingList(trending)))
        .retry()
        .compose(view.bindUntilEvent(View.LifecycleEvent.DESTROY))
        .subscribe(__ -> {
        }, e -> crashReport.log(e));
  }

  @VisibleForTesting public void focusInSearchBar() {
    view.getLifecycleEvent()
        .filter(event -> event.equals(View.LifecycleEvent.CREATE))
        .flatMap(__ -> view.searchSetup())
        .first()
        .filter(__ -> view.shouldFocusInSearchBar())
        .observeOn(viewScheduler)
        .doOnNext(__ -> view.focusInSearchBar())
        .compose(view.bindUntilEvent(View.LifecycleEvent.DESTROY))
        .subscribe(__ -> {
        }, e -> crashReport.log(e));
  }

  @VisibleForTesting public void stopLoadingMoreOnDestroy() {
    view.getLifecycleEvent()
        .filter(event -> event.equals(View.LifecycleEvent.DESTROY))
        .first()
        .toSingle()
        .observeOn(viewScheduler)
        .subscribe(__ -> view.hideLoadingMore(), e -> crashReport.log(e));
  }

  @VisibleForTesting public void handleSearchListReachedBottom() {
    view.getLifecycleEvent()
        .filter(event -> event.equals(View.LifecycleEvent.CREATE))
        .observeOn(viewScheduler)
        .flatMap(__ -> Observable.merge(view.searchResultsReachedBottom(),
            view.changeFilterAfterNoResults()))
        .map(__ -> view.getViewModel())
        .observeOn(viewScheduler)
        .doOnNext(__ -> view.showLoadingMore())
        .flatMapSingle(viewModel -> loadData(viewModel.getSearchQueryModel()
            .getFinalQuery(), viewModel.getStoreName(), viewModel.getFilters(), true))
        .observeOn(viewScheduler)
        .doOnNext(__ -> view.hideLoadingMore())
        .filter(data -> data != null)
        .compose(view.bindUntilEvent(View.LifecycleEvent.DESTROY))
        .subscribe(__ -> {
        }, e -> crashReport.log(e));
  }

  public SearchResultDiffModel getResultList(SearchResult result) {
    SearchResultDiffModel searchResultDiffModel =
        new SearchResultDiffModel(null, Collections.emptyList());
    if (!result.hasError()) {
      searchResultDiffModel = result.getSearchResultDiffModel();
    }
    return searchResultDiffModel;
  }

  @VisibleForTesting public void firstAdsDataLoad() {
    view.getLifecycleEvent()
        .filter(event -> event.equals(View.LifecycleEvent.CREATE))
        .map(__ -> view.getViewModel())
        .filter(viewModel -> hasValidQuery(viewModel))
        .filter(viewModel -> !viewModel.hasLoadedAds())
        .doOnNext(ad -> {
          ad.setHasLoadedAds();
          view.setAllStoresAdsEmpty();
        })
        .flatMapCompletable(__ -> loadBannerAd())
        .compose(view.bindUntilEvent(View.LifecycleEvent.DESTROY))
        .subscribe(__ -> {
        }, e -> crashReport.log(e));
  }

  @VisibleForTesting public boolean hasValidQuery(SearchResultView.Model viewModel) {
    return viewModel.getSearchQueryModel() != null && !viewModel.getSearchQueryModel()
        .getFinalQuery()
        .isEmpty();
  }

  @VisibleForTesting public void handleClickToOpenAppViewFromItem() {
    view.getLifecycleEvent()
        .filter(event -> event.equals(View.LifecycleEvent.CREATE))
        .observeOn(viewScheduler)
        .flatMap(__ -> view.onViewItemClicked())
        .doOnNext(data -> openAppView(data))
        .compose(view.bindUntilEvent(View.LifecycleEvent.DESTROY))
        .subscribe(__ -> {
        }, e -> crashReport.log(e));
  }

  @VisibleForTesting public void handleClickToOpenAppViewFromAdd() {
    view.getLifecycleEvent()
        .filter(event -> event.equals(View.LifecycleEvent.CREATE))
        .observeOn(viewScheduler)
        .flatMap(__ -> view.onAdClicked())
        .doOnNext(data -> {
          analytics.searchAdClick(view.getViewModel()
              .getSearchQueryModel(), data.getSearchAdResult()
              .getPackageName(), data.getPosition(), data.getSearchAdResult()
              .isAppc());
          navigator.goToAppView(data.getSearchAdResult());
        })
        .compose(view.bindUntilEvent(View.LifecycleEvent.DESTROY))
        .subscribe(__ -> {
        }, e -> crashReport.log(e));
  }

  public void handleClickOnAdultContentSwitch() {
    view.getLifecycleEvent()
        .filter(event -> event.equals(View.LifecycleEvent.CREATE))
        .observeOn(viewScheduler)
        .flatMap(__ -> view.clickAdultContentSwitch())
        .observeOn(Schedulers.io())
        .flatMap(isChecked -> {
          if (!isChecked) {
            return searchManager.disableAdultContent()
                .observeOn(viewScheduler)
                .doOnError(e -> view.enableAdultContent())
                .toObservable()
                .map(__ -> false);
          } else {
            return Observable.just(true);
          }
        })
        .observeOn(viewScheduler)
        .filter(show -> show)
        .flatMap(__ -> searchManager.isAdultContentPinRequired())
        .doOnNext(pinRequired -> {
          if (pinRequired) {
            view.showAdultContentConfirmationDialogWithPin();
          } else {
            view.showAdultContentConfirmationDialog();
          }
        })
        .compose(view.bindUntilEvent(View.LifecycleEvent.DESTROY))
        .subscribe(__ -> {
        }, e -> crashReport.log(e));
  }

  private void handleAdultContentDialogPositiveClick() {
    view.getLifecycleEvent()
        .filter(event -> event.equals(View.LifecycleEvent.CREATE))
        .flatMap(__ -> view.adultContentDialogPositiveClick())
        .observeOn(Schedulers.io())
        .flatMapCompletable(click -> searchManager.enableAdultContent())
        .observeOn(viewScheduler)
        .doOnError(e -> view.disableAdultContent())
        .compose(view.bindUntilEvent(View.LifecycleEvent.DESTROY))
        .subscribe(__ -> {
        }, e -> crashReport.log(e));
  }

  private void handleAdultContentDialogNegativeClick() {
    view.getLifecycleEvent()
        .filter(event -> event.equals(View.LifecycleEvent.CREATE))
        .flatMap(__ -> Observable.merge(view.adultContentPinDialogNegativeClick(),
            view.adultContentDialogNegativeClick()))
        .doOnNext(__ -> view.disableAdultContent())
        .compose(view.bindUntilEvent(View.LifecycleEvent.DESTROY))
        .subscribe(__ -> {
        }, e -> crashReport.log(e));
  }

  private void handleAdultContentDialogWithPinPositiveClick() {
    view.getLifecycleEvent()
        .filter(event -> event.equals(View.LifecycleEvent.CREATE))
        .flatMap(__ -> view.adultContentWithPinDialogPositiveClick()
            .observeOn(Schedulers.io())
            .flatMap(pin -> searchManager.enableAdultContentWithPin(pin.toString()
                .isEmpty() ? 0 : Integer.valueOf(pin.toString()))
                .toObservable()
                .observeOn(viewScheduler)
                .doOnError(throwable -> {
                  if (throwable instanceof SecurityException) {
                    view.showWrongPinErrorMessage();
                  }
                }))
            .retry())
        .compose(view.bindUntilEvent(View.LifecycleEvent.DESTROY))
        .subscribe(__ -> {
        }, e -> crashReport.log(e));
  }

  private void openAppView(SearchAppResultWrapper searchApp) {
    final String packageName = searchApp.getSearchAppResult()
        .getPackageName();
    final long appId = searchApp.getSearchAppResult()
        .getAppId();
    final String storeName = searchApp.getSearchAppResult()
        .getStoreName();
    analytics.searchAppClick(view.getViewModel()
        .getSearchQueryModel(), packageName, searchApp.getPosition(), searchApp.getSearchAppResult()
        .isAppcApp());
    navigator.goToAppView(appId, packageName, view.getViewModel()
        .getStoreTheme(), storeName);
  }

  private Single<SearchResult> loadData(String query, String storeName, List<Filter> filters,
      boolean isLoadMore) {
    if (storeName != null && !storeName.trim()
        .equals("")) {
      return loadDataForSpecificStore(query, storeName, filters, isLoadMore);
    }
    // search every store. followed and not followed
    return loadDataFromNonFollowedStores(query, filters, isLoadMore);
  }

  @NonNull
  private Single<SearchResult> loadDataFromNonFollowedStores(String query, List<Filter> filters,
      boolean isLoadMore) {
    return searchManager.searchAppInStores(query, filters)
        .flatMap(searchResult -> mapToViewAndLoadAds(query, searchResult, isLoadMore));
  }

  @NonNull private Single<SearchResult> loadDataForSpecificStore(String query, String storeName,
      List<Filter> filters, boolean isLoadMore) {
    return searchManager.searchInStore(query, storeName, filters)
        .flatMap(searchResult -> mapToViewAndLoadAds(query, searchResult, isLoadMore));
  }

  private Single<SearchResult> mapToViewAndLoadAds(String query, SearchResult searchResult,
      boolean isLoadMore) {
    return Single.just(searchResult)
        .observeOn(viewScheduler)
        .doOnSuccess(
            dataList -> view.addAllStoresResult(query, getResultList(dataList), isLoadMore))
        .observeOn(ioScheduler)
        .flatMap(nonFollowedStoresSearchResult -> searchManager.shouldLoadNativeAds()
            .observeOn(viewScheduler)
            .doOnSuccess(loadNativeAds -> {
              if (loadNativeAds) {
                view.showNativeAds(query);
              }
            })
            .map(__ -> nonFollowedStoresSearchResult));
  }

  private int getResultsCount(SearchResult result) {
    int count = 0;
    if (!result.hasError()) {
      count += result.getSearchResultDiffModel()
          .getSearchResultsList()
          .size();
    }
    return count;
  }

  @VisibleForTesting public void doFirstSearch() {
    view.getLifecycleEvent()
        .filter(event -> event.equals(View.LifecycleEvent.CREATE))
        .map(__ -> view.getViewModel())
        .flatMap(model -> search(model))
        .compose(view.bindUntilEvent(View.LifecycleEvent.DESTROY))
        .subscribe(__ -> {
        }, e -> crashReport.log(e));
  }

  public void redoSearchAfterAdultContentSwitch() {
    view.getLifecycleEvent()
        .filter(event -> event.equals(View.LifecycleEvent.CREATE))
        .flatMap(__ -> Observable.merge(view.adultContentDialogPositiveClick(),
            view.adultContentWithPinDialogPositiveClick()))
        .map(__ -> view.getViewModel())
        .flatMap(model -> search(model))
        .compose(view.bindUntilEvent(View.LifecycleEvent.DESTROY))
        .subscribe(__ -> {
        }, e -> crashReport.log(e));
  }

  public void updateAdultContentSwitchOnNoResults() {
    view.getLifecycleEvent()
        .filter(event -> event.equals(View.LifecycleEvent.CREATE))
        .compose(view.bindUntilEvent(View.LifecycleEvent.DESTROY))
        .flatMap(__ -> view.viewHasNoResults())
        .flatMap(__ -> searchManager.isAdultContentEnabled())
        .doOnNext(adultContent -> view.setAdultContentSwitch(adultContent))
        .subscribe(__ -> {
        }, e -> crashReport.log(e));
  }

  public Observable<SearchResultCount> search(SearchResultView.Model resultModel) {
    return Observable.just(resultModel)
        .filter(viewModel -> hasValidQuery(viewModel))
        .observeOn(viewScheduler)
        .doOnNext(__ -> view.hideSuggestionsViews())
        .doOnNext(__ -> view.showLoading())
        .observeOn(ioScheduler)
        .flatMapSingle(viewModel -> loadData(viewModel.getSearchQueryModel()
            .getFinalQuery(), viewModel.getStoreName(), viewModel.getFilters(), false).observeOn(
            viewScheduler)
            .doOnSuccess(__2 -> view.hideLoading())
            .flatMap(searchResult -> {
              int count = 0;
              if (searchResult.hasError()) {
                if (searchResult.getError() == SearchResultError.NO_NETWORK) {
                  view.showNoNetworkView();
                } else {
                  view.showGenericErrorView();
                }
              } else {
                count = getResultsCount(searchResult);
                if (getResultsCount(searchResult) <= 0) {
                  view.showNoResultsView();
                  analytics.searchNoResults(viewModel.getSearchQueryModel());
                } else {
                  view.showResultsView();
                }
              }
              return Single.just(count);
            })
            .zipWith(Single.just(viewModel),
                (itemCount, model) -> new SearchResultCount(itemCount, model)));
  }

  @VisibleForTesting public void handleQueryTextCleaned() {
    view.getLifecycleEvent()
        .filter(event -> event.equals(View.LifecycleEvent.CREATE))
        .flatMap(__ -> getDebouncedQueryChanges().filter(
            data -> !data.hasQuery() && view.isSearchViewExpanded())
            .observeOn(viewScheduler)
            .doOnNext(data -> {
              view.clearUnsubmittedQuery();
              view.toggleTrendingView();
            }))
        .compose(view.bindUntilEvent(View.LifecycleEvent.DESTROY))
        .subscribe(__ -> {
        }, e -> crashReport.log(e));
  }

  @VisibleForTesting public void handleQueryTextChanged() {
    view.getLifecycleEvent()
        .filter(event -> event.equals(View.LifecycleEvent.CREATE))
        .flatMap(__ -> view.searchSetup())
        .first()
        .flatMap(__ -> getDebouncedQueryChanges())
        .filter(data -> data.hasQuery() && !data.isSubmitted())
        .map(data -> data.getQuery())
        .doOnNext(query -> view.setUnsubmittedQuery(query))
        .flatMapSingle(query -> suggestionManager.getSuggestionsForApp(query)
            .onErrorResumeNext(err -> {
              if (err instanceof TimeoutException) {
                Logger.getInstance()
                    .i(TAG, "Timeout reached while waiting for application suggestions");
              }
              return Single.error(err);
            })
            .observeOn(viewScheduler)
            .doOnSuccess(queryResults -> {
              view.setSuggestionsList(queryResults);
              view.toggleSuggestionsView();
            }))
        .compose(view.bindUntilEvent(View.LifecycleEvent.DESTROY))
        .subscribe(__ -> {
        }, e -> crashReport.log(e));
  }

  @VisibleForTesting public void handleQueryTextSubmitted() {
    view.getLifecycleEvent()
        .filter(event -> event.equals(View.LifecycleEvent.CREATE))
        .flatMap(__ -> view.searchSetup())
        .first()
        .flatMap(__ -> getDebouncedQueryChanges())
        .filter(data -> data.hasQuery() && data.isSubmitted())
        .observeOn(viewScheduler)
        .doOnNext(data -> {
          view.collapseSearchBar(false);
          view.hideSuggestionsViews();
          SearchQueryModel searchQueryModel = new SearchQueryModel(data.getQuery());
          analytics.search(searchQueryModel);
          navigator.navigate(searchQueryModel);
        })
        .compose(view.bindUntilEvent(View.LifecycleEvent.DESTROY))
        .subscribe(__ -> {
        }, e -> crashReport.log(e));
  }

  @VisibleForTesting public void handleSuggestionClicked() {
    view.getLifecycleEvent()
        .filter(event -> event.equals(View.LifecycleEvent.CREATE))
        .flatMap(__ -> view.listenToSuggestionClick())
        .filter(data -> data.second.hasQuery() && data.second.isSubmitted())
        .doOnNext(data -> {
          view.collapseSearchBar(false);
          view.hideSuggestionsViews();
          SearchQueryModel searchQueryModel =
              new SearchQueryModel(data.first, data.second.getQuery(),
                  data.first.isEmpty() ? Source.FROM_TRENDING : Source.FROM_AUTOCOMPLETE);
          navigator.navigate(searchQueryModel);
          analytics.searchFromSuggestion(searchQueryModel, data.second.getPosition());
        })
        .compose(view.bindUntilEvent(View.LifecycleEvent.DESTROY))
        .subscribe(__ -> {
        }, e -> crashReport.log(e));
  }

  @VisibleForTesting public void handleToolbarClick() {
    view.getLifecycleEvent()
        .filter(event -> event == View.LifecycleEvent.CREATE)
        .flatMap(__ -> view.toolbarClick())
        .doOnNext(__ -> {
          if (!view.shouldFocusInSearchBar()) {
            analytics.searchStart(SearchSource.SEARCH_TOOLBAR, true);
          }
        })
        .doOnNext(__ -> view.focusInSearchBar())
        .compose(view.bindUntilEvent(View.LifecycleEvent.DESTROY))
        .subscribe(__ -> {
        }, err -> crashReport.log(err));
  }

  @VisibleForTesting public void handleSearchMenuItemClick() {
    view.getLifecycleEvent()
        .filter(event -> event == View.LifecycleEvent.RESUME)
        .flatMap(__ -> view.searchMenuItemClick())
        .doOnNext(__ -> {
          if (!view.shouldFocusInSearchBar()) analytics.searchStart(SearchSource.SEARCH_ICON, true);
        })
        .doOnNext(__ -> view.focusInSearchBar())
        .compose(view.bindUntilEvent(View.LifecycleEvent.PAUSE))
        .subscribe(__ -> {
        }, err -> crashReport.log(err));
  }

  @VisibleForTesting public void handleClickOnBottomNavWithResults() {
    view.getLifecycleEvent()
        .filter(lifecycleEvent -> lifecycleEvent.equals(View.LifecycleEvent.CREATE))
        .flatMap(created -> bottomNavigator.navigationEvent()
            .filter(navigationEvent -> bottomNavigationMapper.mapItemClicked(navigationEvent)
                .equals(BottomNavigationItem.SEARCH))
            .observeOn(viewScheduler)
            .filter(navigated -> view.hasResults())
            .doOnNext(__ -> view.scrollToTop())
            .retry())
        .compose(view.bindUntilEvent(View.LifecycleEvent.DESTROY))
        .subscribe(__ -> {
        }, throwable -> {
          throw new OnErrorNotImplementedException(throwable);
        });
  }

  @VisibleForTesting public void handleClickOnBottomNavWithoutResults() {
    view.getLifecycleEvent()
        .filter(lifecycleEvent -> lifecycleEvent.equals(View.LifecycleEvent.CREATE))
        .flatMap(created -> bottomNavigator.navigationEvent()
            .filter(navigationEvent -> bottomNavigationMapper.mapItemClicked(navigationEvent)
                .equals(BottomNavigationItem.SEARCH))
            .observeOn(viewScheduler)
            .filter(navigated -> !view.hasResults())
            .doOnNext(__ -> view.focusInSearchBar())
            .retry())
        .compose(view.bindUntilEvent(View.LifecycleEvent.DESTROY))
        .subscribe(__ -> {
        }, throwable -> {
          throw new OnErrorNotImplementedException(throwable);
        });
  }

  @VisibleForTesting public void listenToSearchQueries() {
    view.getLifecycleEvent()
        .filter(event -> event == View.LifecycleEvent.RESUME)
        .flatMap(__ -> view.searchSetup())
        .first()
        .flatMap(__ -> view.queryChanged())
        .doOnNext(event -> view.queryEvent(event))
        .compose(view.bindUntilEvent(View.LifecycleEvent.DESTROY))
        .subscribe(__ -> {
        }, e -> crashReport.log(e));
  }

  @NonNull private Observable<SearchQueryEvent> getDebouncedQueryChanges() {
    return view.onQueryTextChanged()
        .debounce(250, TimeUnit.MILLISECONDS);
  }
}
