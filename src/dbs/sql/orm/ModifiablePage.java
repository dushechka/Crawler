package dbs.sql.orm;

import java.net.URL;
import java.sql.Timestamp;

public class ModifiablePage {
    public int iD;
    public URL url;
    public int siteId;
    public Timestamp foundDateTime;
    public Timestamp lastScanDate;

    public ModifiablePage(int iD, URL url, int siteId, Timestamp foundDateTime, Timestamp lastScanDate) {
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
}
