package cm.aptoide.pt.install;

import android.os.Build;
import cm.aptoide.pt.LifecycleTrackerManager;
import cm.aptoide.pt.database.room.RoomDownload;
import cm.aptoide.pt.logger.Logger;
import cm.aptoide.pt.notification.AptoideNotification;
import cm.aptoide.pt.notification.NotificationProvider;
import cm.aptoide.pt.presenter.Presenter;
import rx.Completable;
import rx.Single;
import rx.subscriptions.CompositeSubscription;

/**
 * Created by tiagopedrinho on 10/10/2018.
 */

public class DownloadsNotificationsPresenter implements Presenter {

  private static final String TAG = DownloadsNotificationsPresenter.class.getSimpleName();
  private DownloadsNotification service;
  private InstallManager installManager;
  private LifecycleTrackerManager lifecycleTrackerManager;
  private NotificationProvider notificationProvider;
  private CompositeSubscription subscriptions;

  public DownloadsNotificationsPresenter(DownloadsNotification service,
      InstallManager installManager, LifecycleTrackerManager lifecycleTrackerManager,
      NotificationProvider notificationProvider) {
    this.service = service;
    this.installManager = installManager;
    this.lifecycleTrackerManager = lifecycleTrackerManager;
    this.notificationProvider = notificationProvider;
    subscriptions = new CompositeSubscription();
  }

  private void handleCurrentInstallation() {
    subscriptions.add(installManager.getCurrentInstallation()
        .doOnError(throwable -> {
          throwable.printStackTrace();
          Logger.getInstance()
              .d(TAG, "error on getCurrentInstallation");
        })
        .doOnNext(installation -> {
          if (!installation.isIndeterminate()) {
            String md5 = installation.getMd5();
            service.setupProgressNotification(md5, installation.getAppName(),
                installation.getProgress(), installation.isIndeterminate());
          }
        })
        .distinctUntilChanged(Install::getState)
        .flatMap(install -> installManager.getDownload(install.getMd5())
            .toObservable())
        .distinctUntilChanged(RoomDownload::getOverallDownloadStatus)
        .flatMapSingle(download -> {
          Install.InstallationStatus installationStatus = installManager.mapDownloadState(download);
          Logger.getInstance()
              .d(TAG, "Received the state " + installationStatus);
          Completable notificationCompletable = Completable.complete();
          if (Build.VERSION.SDK_INT >= 29
              && lifecycleTrackerManager.isAppInBackground()
              && download.getOverallDownloadStatus() == RoomDownload.COMPLETED) {
            notificationCompletable = notificationProvider.save(
                new AptoideNotification(download.getIcon(), download.getAppName(),
                    "aptoideinstall://package="
                        + download.getPackageName()
                        + "&store="
                        + download.getStoreName()
                        + "&open_type=open_and_install", download.getIcon(),
                    AptoideNotification.APPS_READY_TO_INSTALL));
          }
          if (installationStatus != Install.InstallationStatus.DOWNLOADING
              && download.getOverallDownloadStatus() != RoomDownload.WAITING_TO_MOVE_FILES
              && download.getOverallDownloadStatus() != RoomDownload.VERIFYING_FILE_INTEGRITY) {
            Completable finalNotificationCompletable = notificationCompletable;
            return hasNextDownload().flatMap(
                hasNext -> finalNotificationCompletable.andThen(Single.just(hasNext)))
                .doOnSuccess(hasNext -> {
                  Logger.getInstance()
                      .d(TAG, "Has next downloads: " + hasNext);
                  if (!hasNext) {
                    service.removeProgressNotificationAndStop();
                  }
                });
          } else {
            return Single.just(null);
          }
        })
        .subscribe(__ -> {
        }, throwable -> {
          service.removeProgressNotificationAndStop();
          throwable.printStackTrace();
        }));
  }

  public void onDestroy() {
    subscriptions.unsubscribe();
  }

  @Override public void present() {
    handleCurrentInstallation();
  }

  private Single<Boolean> hasNextDownload() {
    return installManager.hasNextDownload();
  }
}
