package cm.aptoide.pt.database.room;

import android.text.TextUtils;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import cm.aptoide.pt.utils.IdUtils;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Entity(tableName = "filetodownload") public class RoomFileToDownload {

  public static final int APK = 0;
  public static final int OBB = 1;
  public static final int GENERIC = 2;
  public static final int SPLIT = 3;

  @PrimaryKey @NonNull private String md5;
  private int downloadId;
  private String altLink;
  private String link;
  private String packageName;
  private String path;
  private @FileType int fileType = GENERIC;
  private int progress;
  private @RoomDownload.DownloadState int status;
  private String fileName;
  private int versionCode;
  private String versionName;

  public static RoomFileToDownload createFileToDownload(String link, String altLink, String md5,
      String fileName, @FileType int fileType, String packageName, int versionCode,
      String versionName, String cachePath) {
    RoomFileToDownload roomFileToDownload = new RoomFileToDownload();
    roomFileToDownload.setLink(link);
    roomFileToDownload.setMd5(md5);
    roomFileToDownload.setAltLink(altLink);
    roomFileToDownload.versionCode = versionCode;
    roomFileToDownload.versionName = versionName;
    roomFileToDownload.setFileType(fileType);
    roomFileToDownload.setPath(cachePath);
    if (!TextUtils.isEmpty(fileName)) {
      if (fileType == APK || fileType == SPLIT) {
        roomFileToDownload.setFileName(fileName + ".apk");
      } else {
        roomFileToDownload.setFileName(fileName);
      }
    }
    roomFileToDownload.setPackageName(packageName);
    return roomFileToDownload;
  }

  public int getVersionCode() {
    return versionCode;
  }

  public String getVersionName() {
    return versionName;
  }

  @Override public int hashCode() {
    int result = getMd5().hashCode();
    result = 31 * result + getDownloadId();
    result = 31 * result + (getAltLink() != null ? getAltLink().hashCode() : 0);
    result = 31 * result + (getLink() != null ? getLink().hashCode() : 0);
    result = 31 * result + (getPackageName() != null ? getPackageName().hashCode() : 0);
    result = 31 * result + (getPath() != null ? getPath().hashCode() : 0);
    result = 31 * result + getFileType();
    result = 31 * result + getProgress();
    result = 31 * result + getStatus();
    result = 31 * result + (getFileName() != null ? getFileName().hashCode() : 0);
    result = 31 * result + getVersionCode();
    result = 31 * result + (getVersionName() != null ? getVersionName().hashCode() : 0);
    return result;
  }

  @Override public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    RoomFileToDownload that = (RoomFileToDownload) o;

    if (getDownloadId() != that.getDownloadId()) return false;
    if (getFileType() != that.getFileType()) return false;
    if (getProgress() != that.getProgress()) return false;
    if (getStatus() != that.getStatus()) return false;
    if (getVersionCode() != that.getVersionCode()) return false;
    if (!getMd5().equals(that.getMd5())) return false;
    if (getAltLink() != null ? !getAltLink().equals(that.getAltLink())
        : that.getAltLink() != null) {
      return false;
    }
    if (getLink() != null ? !getLink().equals(that.getLink()) : that.getLink() != null) {
      return false;
    }
    if (getPackageName() != null ? !getPackageName().equals(that.getPackageName())
        : that.getPackageName() != null) {
      return false;
    }
    if (getPath() != null ? !getPath().equals(that.getPath()) : that.getPath() != null) {
      return false;
    }
    if (getFileName() != null ? !getFileName().equals(that.getFileName())
        : that.getFileName() != null) {
      return false;
    }
    return getVersionName() != null ? getVersionName().equals(that.getVersionName())
        : that.getVersionName() == null;
  }

  public String getAltLink() {
    return altLink;
  }

  private void setAltLink(String altLink) {
    this.altLink = altLink;
  }

  public @RoomDownload.DownloadState int getStatus() {
    return status;
  }

  public void setStatus(@RoomDownload.DownloadState int status) {
    this.status = status;
  }

  public String getLink() {
    return link;
  }

  public void setLink(String link) {
    this.link = link;
  }

  public String getPackageName() {
    return packageName;
  }

  public void setPackageName(String packageName) {
    this.packageName = packageName;
  }

  public int getDownloadId() {
    return downloadId;
  }

  public void setDownloadId(int downloadId) {
    this.downloadId = downloadId;
  }

  public @FileType int getFileType() {
    return fileType;
  }

  public void setFileType(@FileType int fileType) {
    this.fileType = fileType;
  }

  public int getProgress() {
    return progress;
  }

  public void setProgress(int progress) {
    this.progress = progress;
  }

  public String getFilePath() {
    return getPath() + getFileName();
  }

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public String getFileName() {
    if (TextUtils.isEmpty(fileName)) {
      return TextUtils.isEmpty(getMd5()) ? IdUtils.randomString() : getMd5();
    }
    return fileName;
  }

  public void setFileName(String fileName) {
    this.fileName = fileName;
  }

  public String getMd5() {
    return md5;
  }

  public void setMd5(String md5) {
    this.md5 = md5;
  }

  @IntDef({ APK, OBB, GENERIC, SPLIT }) @Retention(RetentionPolicy.SOURCE)
  public @interface FileType {

  }
}
