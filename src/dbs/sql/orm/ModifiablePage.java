package dbs.sql.orm;

import java.sql.Timestamp;

public class ModifiablePage implements Page {
    private int iD;
    private String url;
    private int siteId;
    private Timestamp foundDateTime;
    private Timestamp lastScanDate;

    public ModifiablePage(int iD, String url, int siteId, Timestamp foundDateTime, Timestamp lastScanDate) {
        this.iD = iD;
        this.url = url;
        this.siteId = siteId;
        this.foundDateTime = foundDateTime;
        this.lastScanDate = lastScanDate;
    }

    public ModifiablePage(Page prototype) {
        this.iD = prototype.getiD();
        this.url = prototype.getUrl();
        this.siteId = prototype.getSiteId();
        this.foundDateTime = prototype.getFoundDateTime();
        this.lastScanDate = prototype.getLastScanDate();
    }

    @Override
    public int getiD() {
        return iD;
    }

    @Override
    public String getUrl() {
        return url;
    }

    @Override
    public int getSiteId() {
        return siteId;
    }

    @Override
    public Timestamp getFoundDateTime() {
        return foundDateTime;
    }

    @Override
    public Timestamp getLastScanDate() {
        return lastScanDate;
    }

    public void setiD(int iD) {
        this.iD = iD;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setSiteId(int siteId) {
        this.siteId = siteId;
    }

    public void setFoundDateTime(Timestamp foundDateTime) {
        this.foundDateTime = foundDateTime;
    }

    public void setLastScanDate(Timestamp lastScanDate) {
        this.lastScanDate = lastScanDate;
    }
}
