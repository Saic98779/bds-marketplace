package com.sidbi.bds.common.report;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ReportTable {

    private final String title;
    private final List<String> headers;
    private final List<List<Object>> rows = new ArrayList<>();

    public ReportTable(String title, String... headers) {
        this.title = title;
        this.headers = Arrays.asList(headers);
    }

    public ReportTable row(Object... values) {
        rows.add(Arrays.asList(values));
        return this;
    }

    public String getTitle() {
        return title;
    }

    public List<String> getHeaders() {
        return headers;
    }

    public List<List<Object>> getRows() {
        return rows;
    }
}
