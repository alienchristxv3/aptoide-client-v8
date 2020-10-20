package cm.aptoide.pt.donations;

import java.util.List;

public class GetDonations extends BaseResponse {

  private String next;
  private List<Donor> items;

  public GetDonations() {
  }

  public String getNext() {
    return next;
  }

  public void setNext(String next) {
    this.next = next;
  }

  public List<Donor> getItems() {
    return items;
  }

  public void setItems(List<Donor> items) {
    this.items = items;
  }

  public static class Donor {
    private String domain;
    private String owner;
    private String appc;

    public Donor() {
    }

    public String getDomain() {
      return domain;
    }

    public void setDomain(String domain) {
      this.domain = domain;
    }

    public String getOwner() {
      return owner;
    }

    public void setOwner(String owner) {
      this.owner = owner;
    }

    public String getAppc() {
      return appc;
    }

    public void setAppc(String appc) {
      this.appc = appc;
    }
  }
}
