package com.retailpulse.service.report;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

public abstract class AbstractReportExportService<T> {
    /**
     * Template method for exporting a report using a List of generic type T.
     */
    public final void exportReport(Instant start, Instant end, List<T> data) throws IOException {
        writeReportHeader(start, end);
        writeTableHeader();
        writeTableData(data);
        finalizeReport();
    }

    protected abstract void writeReportHeader(Instant start, Instant end) throws IOException;

    protected abstract void writeTableHeader() throws IOException;

    protected abstract void writeTableData(List<T> data) throws IOException;

    protected abstract void finalizeReport() throws IOException;
}
