package dbs.sql.orm;

import java.sql.Timestamp;

public interface Page {
    int getiD();

    String getUrl();

    int getSiteId();

    Timestamp getFoundDateTime();

    Timestamp getLastScanDate();
}
