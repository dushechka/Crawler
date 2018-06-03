package dbs.sql.orm;

import java.net.URL;
import java.sql.Timestamp;

public class Page {
    private final int iD;
    private final URL url;
    private final int siteId;
    private final Timestamp foundDateTime;
    private final Timestamp lastScanDate;

    public Page(int iD, URL url, int siteId, Timestamp foundDateTime, Timestamp lastScanDate) {
        this.iD = iD;
        this.url = url;
        this.siteId = siteId;
        this.foundDateTime = foundDateTime;
        this.lastScanDate = lastScanDate;
    }

    public int getiD() {
        return iD;
    }

    public URL getUrl() {
        return url;
    }

    public int getSiteId() {
        return siteId;
    }

    public Timestamp getFoundDateTime() {
        return foundDateTime;
    }

    public Timestamp getLastScanDate() {
        return lastScanDate;
    }
}
