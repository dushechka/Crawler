/*
 * Copyright (c) 2018 Andrey Fadeev
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.andreyfadeev.crawler.dbs.sql.orm;

import java.sql.Timestamp;


/**
 * Modifiable pojo orm web-page class.
 */
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
